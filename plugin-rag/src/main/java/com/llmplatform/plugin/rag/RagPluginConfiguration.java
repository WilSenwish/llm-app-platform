package com.llmplatform.plugin.rag;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 插件自动装配。扫描本插件包下的 Bean（如 {@link RagPlugin}），供平台内核导入。
 */
@Configuration
@ComponentScan("com.llmplatform.plugin.rag")
public class RagPluginConfiguration {
}
