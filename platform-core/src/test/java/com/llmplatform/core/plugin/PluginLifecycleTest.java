package com.llmplatform.core.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PluginLifecycleTest {

    @Test
    void springPluginWinsAndSpiDuplicateIsSkipped() {
        RecordingPlugin spring = new RecordingPlugin("shared");
        RecordingPlugin duplicateSpi = new RecordingPlugin("shared");
        RecordingPlugin spiOnly = new RecordingPlugin("spi-only");
        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        PluginLifecycle lifecycle = new PluginLifecycle(
                registry,
                ObservationRegistry.create(),
                List.of(spring),
                () -> List.of(duplicateSpi, spiOnly));

        lifecycle.bootstrap();

        assertEquals(2, registry.size());
        assertTrue(registry.get("shared").isPresent());
        assertTrue(registry.get("spi-only").isPresent());
        assertEquals(1, spring.inits.get());
        assertEquals(0, duplicateSpi.inits.get());
        assertEquals(1, spiOnly.inits.get());
        assertTrue(registry.get("shared").orElseThrow() == spring);
    }

    @Test
    void bootstrapIsIdempotent() {
        RecordingPlugin spring = new RecordingPlugin("once");
        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        PluginLifecycle lifecycle = new PluginLifecycle(
                registry, ObservationRegistry.create(), List.of(spring), List::of);

        lifecycle.bootstrap();
        lifecycle.bootstrap();

        assertEquals(1, spring.inits.get());
        assertEquals(1, registry.size());
    }

    static final class RecordingPlugin implements AppPlugin {
        private final String id;
        private final AtomicInteger inits = new AtomicInteger();

        RecordingPlugin(String id) {
            this.id = id;
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata(id, id, "1", id, PluginType.CUSTOM);
        }

        @Override
        public void init(PluginContext context) {
            inits.incrementAndGet();
        }

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.success(id);
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
