package com.llmplatform.gateway;

import com.llmplatform.core.PlatformCoreConfiguration;
import com.llmplatform.gateway.config.GatewayProperties;
import com.llmplatform.plugin.rag.RagPluginConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * 网关进程。导入内核与 RAG 插件，模型调用留在本模块。
 */
@SpringBootApplication
@EnableConfigurationProperties(GatewayProperties.class)
@Import({PlatformCoreConfiguration.class, RagPluginConfiguration.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
