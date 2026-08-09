package com.example.secdsp.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Map Railway/Render postgres URLs → spring.datasource.* before DataSource auto-config.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE = "railwayDatabaseUrl";

    private static final String[] URL_KEYS = {
            "SPRING_DATASOURCE_URL",
            // Prefer private networking first (faster, fewer SSL hangs than public proxy)
            "DATABASE_URL",
            "DATABASE_PRIVATE_URL",
            "POSTGRES_URL",
            "POSTGRES_PRIVATE_URL",
            "DATABASE_PUBLIC_URL",
            "POSTGRES_PUBLIC_URL",
            "JDBC_DATABASE_URL"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        System.out.println("[datasource] EnvironmentPostProcessor running");

        // Skip processor for local development with explicit config
        String localProfile = environment.getProperty("spring.profiles.active", "");
        if (localProfile.contains("dev") || localProfile.contains("local")) {
            System.out.println("[datasource] Skipping processor for local development (profile: " + localProfile + ")");
            return;
        }

        for (String key : URL_KEYS) {
            String raw = firstEnv(environment, key);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            raw = raw.trim();
            System.out.println("[datasource] Trying " + key + " len=" + raw.length());
            if (raw.startsWith("jdbc:postgresql://")) {
                apply(environment, raw,
                        firstEnv(environment, "SPRING_DATASOURCE_USERNAME", "PGUSER", "DATABASE_USER"),
                        firstEnv(environment, "SPRING_DATASOURCE_PASSWORD", "PGPASSWORD", "DATABASE_PASSWORD"));
                return;
            }
            if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
                if (applyParsed(environment, raw)) {
                    return;
                }
            }
        }

        Map<String, String> env = System.getenv();
        if (env != null) {
            for (Map.Entry<String, String> e : env.entrySet()) {
                String name = e.getKey();
                String val = e.getValue();
                if (!StringUtils.hasText(val) || name.toUpperCase().contains("REDIS")) {
                    continue;
                }
                String v = val.trim();
                if (v.startsWith("postgres://") || v.startsWith("postgresql://")) {
                    System.out.println("[datasource] Scan hit " + name);
                    if (applyParsed(environment, v)) {
                        return;
                    }
                }
                if (v.startsWith("jdbc:postgresql://")) {
                    apply(environment, v, null, null);
                    return;
                }
            }
        }

        String host = firstEnv(environment, "PGHOST", "POSTGRES_HOST", "DB_HOST", "DATABASE_HOST");
        if (!StringUtils.hasText(host)) {
            System.out.println("[datasource] WARN: no DB URL/PGHOST — app will fail DataSource config");
            System.out.println("[datasource] Railway Variables → Add Reference:");
            System.out.println("[datasource]   DATABASE_PUBLIC_URL = (Postgres).DATABASE_PUBLIC_URL");
            System.out.println("[datasource] Private DATABASE_URL is often EMPTY without private networking.");
            dumpDbRelatedEnvKeys();
            excludeRedisIfMissing(environment);
            return;
        }
        String port = firstNonBlank(firstEnv(environment, "PGPORT", "POSTGRES_PORT", "DB_PORT", "DATABASE_PORT"), "5432");
        String db = firstNonBlank(firstEnv(environment, "PGDATABASE", "POSTGRES_DB", "DATABASE_NAME", "DB_NAME"), "railway");
        String user = firstEnv(environment, "PGUSER", "POSTGRES_USER", "DATABASE_USER", "DB_USER");
        String pass = firstEnv(environment, "PGPASSWORD", "POSTGRES_PASSWORD", "DATABASE_PASSWORD", "DB_PASSWORD");
        String ssl = host.contains("railway.internal") ? "prefer" : "require";
        apply(environment, "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=" + ssl, user, pass);
    }

    private boolean applyParsed(ConfigurableEnvironment environment, String raw) {
        try {
            String rest = raw.contains("://") ? raw.substring(raw.indexOf("://") + 3) : raw;
            // lastIndexOf: password may contain '@' if poorly encoded
            int at = rest.lastIndexOf('@');
            if (at < 0) {
                System.out.println("[datasource] parse fail: no @ in URL");
                return false;
            }
            String userpass = rest.substring(0, at);
            String hostdb = rest.substring(at + 1);
            int colon = userpass.indexOf(':');
            String user = colon >= 0 ? userpass.substring(0, colon) : userpass;
            String pass = colon >= 0 ? userpass.substring(colon + 1) : "";
            pass = URLDecoder.decode(pass, StandardCharsets.UTF_8);
            user = URLDecoder.decode(user, StandardCharsets.UTF_8);

            String hostPortDb = hostdb;
            String query = null;
            int q = hostdb.indexOf('?');
            if (q >= 0) {
                query = hostdb.substring(q + 1);
                hostPortDb = hostdb.substring(0, q);
            }
            String host;
            int port = 5432;
            String db;
            int slash = hostPortDb.indexOf('/');
            String hostPort = slash >= 0 ? hostPortDb.substring(0, slash) : hostPortDb;
            db = slash >= 0 ? hostPortDb.substring(slash + 1) : "railway";
            if (hostPort.startsWith("[")) {
                int end = hostPort.indexOf(']');
                host = hostPort.substring(1, end);
                if (end + 1 < hostPort.length() && hostPort.charAt(end + 1) == ':') {
                    port = Integer.parseInt(hostPort.substring(end + 2));
                }
            } else {
                int hc = hostPort.lastIndexOf(':');
                if (hc >= 0) {
                    host = hostPort.substring(0, hc);
                    port = Integer.parseInt(hostPort.substring(hc + 1));
                } else {
                    host = hostPort;
                }
            }

            StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                    .append(host).append(':').append(port).append('/').append(db);
            boolean hasSsl = query != null && query.contains("sslmode=");
            if (StringUtils.hasText(query)) {
                jdbc.append('?').append(query);
                if (!hasSsl) {
                    jdbc.append(host.contains("railway.internal") ? "&sslmode=prefer" : "&sslmode=require");
                }
            } else {
                jdbc.append(host.contains("railway.internal") ? "?sslmode=prefer" : "?sslmode=require");
            }
            apply(environment, jdbc.toString(), user, pass);
            return true;
        } catch (Exception ex) {
            System.out.println("[datasource] parse failed: " + ex.getMessage());
            return false;
        }
    }

    private void apply(ConfigurableEnvironment environment, String url, String user, String pass) {
        url = ensureJdbcTimeouts(url);
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.url", url);
        if (StringUtils.hasText(user)) {
            map.put("spring.datasource.username", user);
        }
        if (pass != null) {
            map.put("spring.datasource.password", pass);
        }
        String redis = firstEnv(environment, "REDIS_URL", "REDIS_PRIVATE_URL", "REDIS_PUBLIC_URL", "SPRING_DATA_REDIS_URL");
        if (StringUtils.hasText(redis)) {
            map.put("spring.data.redis.url", redis.trim());
            System.out.println("[datasource] Redis URL configured");
        } else {
            putRedisExclude(map);
            System.out.println("[datasource] No REDIS_URL — excluding Redis auto-config");
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, map));
        System.out.println("[datasource] OK url=" + url.replaceAll("//[^@]+@", "//***@"));
    }

    /** Avoid infinite SSL/read hangs on Railway public proxy during Flyway boot. */
    private static String ensureJdbcTimeouts(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith("jdbc:postgresql://")) {
            return url;
        }
        StringBuilder extra = new StringBuilder();
        if (!url.contains("connectTimeout=")) {
            extra.append("connectTimeout=10");
        }
        if (!url.contains("socketTimeout=")) {
            if (extra.length() > 0) extra.append('&');
            extra.append("socketTimeout=45");
        }
        if (!url.contains("loginTimeout=")) {
            if (extra.length() > 0) extra.append('&');
            extra.append("loginTimeout=10");
        }
        if (extra.length() == 0) {
            return url;
        }
        return url.contains("?") ? url + "&" + extra : url + "?" + extra;
    }

    private void excludeRedisIfMissing(ConfigurableEnvironment environment) {
        if (StringUtils.hasText(firstEnv(environment, "REDIS_URL", "REDIS_PRIVATE_URL", "REDIS_PUBLIC_URL"))) {
            return;
        }
        Map<String, Object> map = new HashMap<>();
        putRedisExclude(map);
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE + "RedisExclude", map));
    }

    private static void putRedisExclude(Map<String, Object> map) {
        map.put(
                "spring.autoconfigure.exclude",
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration");
    }

    private static void dumpDbRelatedEnvKeys() {
        Map<String, String> env = System.getenv();
        if (env == null) {
            return;
        }
        System.out.println("[datasource] DB-related env keys present:");
        env.keySet().stream()
            .filter(k -> {
                String u = k.toUpperCase();
                return u.contains("DATABASE") || u.contains("POSTGRES") || u.startsWith("PG")
                    || u.contains("JDBC") || u.contains("DATASOURCE");
            })
            .sorted()
            .forEach(k -> {
                String v = env.get(k);
                String state = !StringUtils.hasText(v) ? "EMPTY" : ("SET len=" + v.trim().length());
                System.out.println("[datasource]   " + k + "=" + state);
            });
    }

    private static String firstEnv(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String fromEnv = System.getenv(key);
            if (StringUtils.hasText(fromEnv)) {
                return fromEnv;
            }
            String fromProps = environment.getProperty(key);
            if (StringUtils.hasText(fromProps)) {
                return fromProps;
            }
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        return StringUtils.hasText(a) ? a : b;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
