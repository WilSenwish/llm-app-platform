package com.llmplatform.core.plugin;

/**
 * 平台内核统一错误码。
 * <p>插件业务错误码由各插件自行定义，不放进本类，避免内核依赖具体插件。
 */
public final class PlatformErrorCodes {

    /** 请求为空，或缺少 traceId / userId / taskType / payload。 */
    public static final String INVALID_REQUEST = "INVALID_REQUEST";

    /** 没有路由规则能把该 taskType 指到某个插件。 */
    public static final String ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND";

    /** 路由指向的插件尚未注册。 */
    public static final String PLUGIN_NOT_FOUND = "PLUGIN_NOT_FOUND";

    /** 插件执行过程抛出未捕获异常。 */
    public static final String PLUGIN_EXECUTION_FAILED = "PLUGIN_EXECUTION_FAILED";

    /** 熔断打开或舱壁满导致的快速失败（故障隔离层返回）。 */
    public static final String FAULT_ISOLATED = "FAULT_ISOLATED";

    private PlatformErrorCodes() {
    }
}
