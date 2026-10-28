package com.llmplatform.gateway.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关配置。api-key 为空时不鉴权，仅供本地；生产通过环境变量注入。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "llm.gateway")
public class GatewayProperties {

    /** 空字符串表示关闭 API Key 校验。 */
    private String apiKey = "";

    /** 全局每秒放行数，也是桶容量。 */
    private int globalPermitsPerSecond = 20;

    /** 单个用户每秒放行数。 */
    private int userPermitsPerSecond = 5;

    /** 同一个端点的最大尝试次数，含第一次。代码里会再限制到 1–3。 */
    private int maxAttempts = 2;

    /** 单次模型调用超时。 */
    private Duration modelTimeout = Duration.ofSeconds(3);

    /** 按顺序尝试。前面的端点失败或被标记为不健康后，才用后面的。 */
    private List<Endpoint> endpoints = new ArrayList<>();

    /** 一个模型端点。provider 只接受 ollama 或 openai。 */
    @Getter
    @Setter
    public static class Endpoint {

        private String id;
        private String provider = "ollama";
        private String baseUrl;
        private String model;
        private String apiKey = "";
    }
}
