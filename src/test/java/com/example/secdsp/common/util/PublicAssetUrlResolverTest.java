package com.example.secdsp.common.util;

import com.example.secdsp.config.UploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicAssetUrlResolverTest {

    private PublicAssetUrlResolver resolver;

    @BeforeEach
    void setUp() {
        UploadProperties props = new UploadProperties();
        props.setPublicBaseUrl("https://sedsp-api-production.up.railway.app");
        resolver = new PublicAssetUrlResolver(props);
    }

    @Test
    void resolvesRelativeUploadPath() {
        assertEquals(
            "https://sedsp-api-production.up.railway.app/uploads/products/a.jpg",
            resolver.resolve("/uploads/products/a.jpg")
        );
    }

    @Test
    void resolvesLocalhostUploadPath() {
        assertEquals(
            "https://sedsp-api-production.up.railway.app/uploads/momo/qr.png",
            resolver.resolve("http://localhost:8080/uploads/momo/qr.png")
        );
    }

    @Test
    void keepsExternalUrls() {
        String cdn = "https://res.cloudinary.com/demo/image/upload/v1/x.jpg";
        assertEquals(cdn, resolver.resolve(cdn));
    }

    @Test
    void nullAndBlankPassThrough() {
        assertNull(resolver.resolve(null));
        assertEquals("", resolver.resolve("  "));
    }
}
