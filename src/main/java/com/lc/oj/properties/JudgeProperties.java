package com.lc.oj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "lcoj.judge")
@Data
public class JudgeProperties {
    private String dataPath;
    private boolean rapidApi;
    private String localUrl;
    private String apiUrl;
    private List<String> xRapidapiKey;
    private String xRapidapiHost;
}
