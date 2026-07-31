package com.example.secdsp.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.powerbi")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PowerBiProperties {

    /**
     * Optional published report embed URL (Power BI Service → Embed).
     * FE can iframe when set.
     */
    String embedUrl = "";

    /** Workspace / report labels for UI */
    String reportTitle = "SEDSP Decision Dashboard";
}
