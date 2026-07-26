package com.example.secdsp.config;

import java.net.URI;
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
 * Railway / PaaS: map postgres:// DATABASE_* URLs into spring.datasource.*.
 * Prefer PUBLIC URL when private networking URL is empty.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE = "railwayDatabaseUrl";

    private static final String[] URL_KEYS = {
            "SPRING_DATASOURCE_URL",
            "DATABASE_PUBLIC_URL",
            "POSTGRES_PUBLIC_URL",
            "DATABASE_PRIVATE_URL",
            "DATABASE_URL",
            "POSTGRES_URL",
            "POSTGRES_PRIVATE_URL",
            "JDBC_DATABASE_URL"
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        for (String key : URL_KEYS) {
            String raw = firstEnv(environment, key);
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            raw = raw.trim();
            System.out.println("[datasource] Using " + key + " (len=" + raw.length() + ")");
            if (raw.startsWith("jdbc:postgresql://")) {
                apply(environment, raw, firstEnv(environment, "SPRING_DATASOURCE_USERNAME"),
                        firstEnv(environment, "SPRING_DATASOURCE_PASSWORD"));
                return;
            }
            if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
                if (applyParsed(environment, raw)) {
                    return;
                }
            }
            if (raw.startsWith("${{") || raw.startsWith("${")) {
                System.out.println("[datasource] WARN " + key + " looks unresolved: " + raw);
            }
        }

        // Scan all env for any postgres:// (custom Railway variable names)
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
                    System.out.println("[datasource] Found postgres URL in " + name);
                    if (applyParsed(environment, v)) {
                        return;
                    }
                }
                if (v.startsWith("jdbc:postgresql://")) {
                    System.out.println("[datasource] Found JDBC URL in " + name);
                    apply(environment, v, null, null);
                    return;
                }
            }
        }

        String host = firstEnv(environment, "PGHOST", "POSTGRES_HOST", "DB_HOST");
        if (!StringUtils.hasText(host)) {
            System.out.println("[datasource] WARN: no DATABASE_URL / DATABASE_PUBLIC_URL / PGHOST found");
            return;
        }
        String port = firstEnv(environment, "PGPORT", "POSTGRES_PORT", "DB_PORT");
        if (!StringUtils.hasText(port)) {
            port = "5432";
        }
        String db = firstEnv(environment, "PGDATABASE", "POSTGRES_DB", "POSTGRES_DATABASE", "DB_NAME");
        if (!StringUtils.hasText(db)) {
            db = "railway";
        }
        String user = firstEnv(environment, "PGUSER", "POSTGRES_USER", "DB_USER");
        String pass = firstEnv(environment, "PGPASSWORD", "POSTGRES_PASSWORD", "DB_PASSWORD");
        String ssl = host.contains("railway.internal") ? "prefer" : "require";
        String jdbc = "jdbc:postgresql://" + host + ":" + port + "/" + db + "?sslmode=" + ssl;
        System.out.println("[datasource] Built JDBC from PGHOST=" + host);
        apply(environment, jdbc, user, pass);
    }

    private boolean applyParsed(ConfigurableEnvironment environment, String raw) {
        try {
            String normalized = raw.replace("postgres://", "postgresql://");
            URI uri = URI.create(normalized);
            if (uri.getHost() == null || uri.getUserInfo() == null) {
                return false;
            }
            String userInfo = uri.getUserInfo();
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            String pass = colon >= 0 ? userInfo.substring(colon + 1) : "";
            pass = URLDecoder.decode(pass, StandardCharsets.UTF_8);

            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() == null ? "" : uri.getPath();
            String db = path.startsWith("/") ? path.substring(1) : path;
            int q = db.indexOf('?');
            if (q >= 0) {
                db = db.substring(0, q);
            }
            String query = uri.getQuery();
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
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, map));
        String safe = url.replaceAll("//[^@]+@", "//***@");
        System.out.println("[datasource] spring.datasource.url=" + safe);
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
