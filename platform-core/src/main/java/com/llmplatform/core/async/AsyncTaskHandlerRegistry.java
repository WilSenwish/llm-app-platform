package com.llmplatform.core.async;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步任务处理器注册表。
 * <p>Spring 容器启动时自动收集所有 {@link AsyncTaskHandler} Bean 并按 taskType 注册。
 */
@Slf4j
public class AsyncTaskHandlerRegistry {

    private final Map<String, AsyncTaskHandler> handlers = new ConcurrentHashMap<>();

    public AsyncTaskHandlerRegistry(List<AsyncTaskHandler> handlers) {
        if (handlers != null) {
            for (AsyncTaskHandler h : handlers) {
                register(h);
            }
        }
    }

    public void register(AsyncTaskHandler handler) {
        AsyncTaskHandler prev = handlers.put(handler.taskType(), handler);
        if (prev != null) {
            log.warn("[AsyncRegistry] taskType={} 处理器被覆盖: {} → {}",
                    handler.taskType(), prev.getClass().getSimpleName(), handler.getClass().getSimpleName());
        } else {
            log.info("[AsyncRegistry] 注册处理器 taskType={} handler={}",
                    handler.taskType(), handler.getClass().getSimpleName());
        }
    }

    public AsyncTaskHandler find(String taskType) {
        return handlers.get(taskType);
    }

    public boolean has(String taskType) {
        return handlers.containsKey(taskType);
    }

    public int size() {
        return handlers.size();
    }
}
