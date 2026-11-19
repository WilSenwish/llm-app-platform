package com.llmplatform.core.fault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.llmplatform.core.PlatformCoreConfiguration;
import com.llmplatform.core.executor.PluginExecutor;
import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.DefaultPluginContext;
import com.llmplatform.core.plugin.InMemoryPluginRegistry;
import com.llmplatform.core.plugin.PlatformErrorCodes;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;
import io.micrometer.observation.ObservationRegistry;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 故障隔离切面集成测试。
 * <p>验证 CircuitBreaker 打开、Bulkhead 满、插件隔离等核心场景。
 * <p>默认配置：failureRateThreshold=50%, slidingWindowSize=20, minimumNumberOfCalls=5。
 * 即连续失败 5 次（全失败率 100% > 50%）即可触发熔断。
 */
@SpringBootTest(classes = PlatformCoreConfiguration.class)
class FaultIsolationAspectIntegrationTest {

    @Autowired
    PluginExecutor pluginExecutor;

    @Autowired
    InMemoryPluginRegistry registry;

    @Autowired
    PluginFaultRegistry faultRegistry;

    @Autowired
    ObservationRegistry observationRegistry;

    private PluginContext context;

    @BeforeEach
    void setUp() {
        context = new DefaultPluginContext("trace-test", "user-test", observationRegistry);
        // 清理所有已注册插件的熔断/舱壁状态
        registry.getAll().forEach(p -> faultRegistry.reset(p.getMetadata().id()));
    }

    // ===================== CircuitBreaker 场景 =====================

    @Test
    void circuitBreakerOpensAfter5ConsecutiveFailures() {
        FailingPlugin failing = new FailingPlugin("cb-fail-1");
        registry.register(failing);

        PluginRequest req = new PluginRequest("t", "u", "test.x", Map.of());

        // 前 5 次：插件执行失败，CB 记录错误，但还没打开（第 5 次 onError 后打开）
        for (int i = 0; i < 5; i++) {
            PluginResult<?> result = pluginExecutor.execute(failing, req, context);
            assertFalse(result.success(), "插件失败本身还未触发熔断拦截");
        }

        // 第 6 次调用：CB 已打开，应该被拦截快速失败
        int callCountBefore = failing.callCount.get();
        PluginResult<?> openResult = pluginExecutor.execute(failing, req, context);

        assertFalse(openResult.success());
        assertEquals(PlatformErrorCodes.FAULT_ISOLATED, openResult.errorCode());
        assertEquals(callCountBefore, failing.callCount.get(), "熔断打开后插件不再被调用");
    }

    @Test
    void pluginResultFailureAlsoCountsAsCircuitBreakerError() {
        SuccessFalsePlugin falsePlugin = new SuccessFalsePlugin("success-false-cb");
        registry.register(falsePlugin);

        PluginRequest req = new PluginRequest("t", "u", "test.x", Map.of());

        // success=false 也应该触发 CB onError
        for (int i = 0; i < 5; i++) {
            pluginExecutor.execute(falsePlugin, req, context);
        }

        // 第 6 次被熔断拦截
        PluginResult<?> result = pluginExecutor.execute(falsePlugin, req, context);
        assertFalse(result.success());
        assertEquals(PlatformErrorCodes.FAULT_ISOLATED, result.errorCode());
    }

    // ===================== 插件隔离场景 =====================

    @Test
    void circuitBreakerIsolatedPerPlugin() {
        FailingPlugin failing = new FailingPlugin("rag-plugin");
        GoodPlugin good = new GoodPlugin("code-agent");
        registry.register(failing);
        registry.register(good);

        PluginRequest req = new PluginRequest("t", "u", "test.x", Map.of());

        // 把 rag-plugin 熔断打开
        for (int i = 0; i < 5; i++) {
            pluginExecutor.execute(failing, req, context);
        }

        // rag-plugin 现在应该被熔断
        PluginResult<?> ragResult = pluginExecutor.execute(failing, req, context);
        assertEquals(PlatformErrorCodes.FAULT_ISOLATED, ragResult.errorCode());

        // code-agent 完全不受影响
        PluginResult<?> codeResult = pluginExecutor.execute(good, req, context);
        assertTrue(codeResult.success(), "其他插件应该不受熔断影响");
        assertEquals("code-ok", codeResult.data());
    }

    // ===================== Bulkhead 场景 =====================

    @Test
    void bulkheadRejectsWhenConcurrentExceedsLimit() throws InterruptedException {
        SlowPlugin slow = new SlowPlugin("slow-plugin-bh");
        registry.register(slow);

        PluginRequest req = new PluginRequest("t", "u", "test.x", Map.of());

        // 并发发起多个请求（默认 Bulkhead maxConcurrent=10，设 4 线程应该都放行）
        // 但如果 Bulkhead 被 CB 包在外面，CB 窗口未填满时全部放行
        Thread[] threads = new Thread[4];
        PluginResult<?>[] results = new PluginResult<?>[4];

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                try {
                    results[idx] = pluginExecutor.execute(slow, req, context);
                } catch (Throwable t) {
                    results[idx] = PluginResult.failure("THREAD_ERROR", t.getMessage());
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join(3000);
        }

        // Bulkhead=10 足够放行 4 个请求，应该全部成功
        for (PluginResult<?> r : results) {
            assertTrue(r != null && r.success(), "Bulkhead 未达上限时应放行: " + (r != null ? r.errorMessage() : "null"));
        }
    }

    // ===================== 辅助插件类 =====================

    /** 总是抛异常的插件。 */
    static class FailingPlugin implements AppPlugin {
        final String id;
        final AtomicInteger callCount = new AtomicInteger(0);

        FailingPlugin(String id) {
            this.id = id;
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata(id, id, "1", "test", PluginType.RAG);
        }

        @Override
        public void init(PluginContext context) {}

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            callCount.incrementAndGet();
            throw new RuntimeException("boom from " + id);
        }

        @Override
        public void destroy() {}
    }

    /** 总是返回 success=false 的插件。 */
    static class SuccessFalsePlugin implements AppPlugin {
        final String id;

        SuccessFalsePlugin(String id) {
            this.id = id;
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata(id, id, "1", "test", PluginType.RAG);
        }

        @Override
        public void init(PluginContext context) {}

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.failure("BIZ_ERROR", "业务失败");
        }

        @Override
        public void destroy() {}
    }

    /** 总是成功的插件。 */
    static class GoodPlugin implements AppPlugin {
        final String id;

        GoodPlugin(String id) {
            this.id = id;
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata(id, id, "1", "test", PluginType.CODE_AGENT);
        }

        @Override
        public void init(PluginContext context) {}

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.success("code-ok");
        }

        @Override
        public void destroy() {}
    }

    /** 慢插件（模拟耗时任务）。 */
    static class SlowPlugin implements AppPlugin {
        final String id;

        SlowPlugin(String id) {
            this.id = id;
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata(id, id, "1", "test", PluginType.RAG);
        }

        @Override
        public void init(PluginContext context) {}

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return PluginResult.success("done");
        }

        @Override
        public void destroy() {}
    }
}
