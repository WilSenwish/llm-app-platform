package com.llmplatform.core.plugin;

/**
 * 平台统一插件 SPI（Service Provider Interface）。
 * <p>所有业务插件（RAG、CodeAgent、Extractor、MultiAgent 等）均实现该接口，
 * 由平台内核的插件注册中心统一管理生命周期（init / destroy）与调用（execute）。
 * <p>设计要点：
 * <ul>
 *   <li>内核与业务解耦，内核不感知任何具体插件实现；</li>
 *   <li>插件通过 {@link PluginContext} 获取底座能力，禁止反向依赖内核内部类；</li>
 *   <li>入参 / 出参遵循 {@link PluginRequest} / {@link PluginResult} 统一契约，便于埋点与治理。</li>
 * </ul>
 */
public interface AppPlugin {

    /** 插件元信息（身份、版本、类型）。 */
    PluginMetadata getMetadata();

    /** 插件初始化，由注册中心在加载时调用，注入运行时上下文。 */
    void init(PluginContext context);

    /** 执行具体任务，平台内核通过前置编排层统一调度。 */
    PluginResult<?> execute(PluginRequest request);

    /** 插件销毁，由注册中心在卸载时调用，释放外部资源。 */
    void destroy();
}
