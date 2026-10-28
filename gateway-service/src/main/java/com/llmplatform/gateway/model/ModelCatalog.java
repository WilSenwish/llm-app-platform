package com.llmplatform.gateway.model;

import java.util.List;

/**
 * 当前进程要路由的端点清单。测试可以替换这个 Bean，避免连真实模型。
 */
public interface ModelCatalog {

    List<ChatModel> models();
}
