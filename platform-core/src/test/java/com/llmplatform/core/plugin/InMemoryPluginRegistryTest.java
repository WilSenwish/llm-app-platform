package com.llmplatform.core.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryPluginRegistryTest {

    @Test
    void registerAndGetAndUnregister() {
        InMemoryPluginRegistry registry = new InMemoryPluginRegistry();
        AppPlugin stub = new StubPlugin();
        registry.register(stub);

        assertTrue(registry.contains("stub"));
        assertEquals(1, registry.size());
        assertTrue(registry.get("stub").isPresent());
        assertEquals(1, registry.getAll().size());

        registry.unregister("stub");
        assertFalse(registry.contains("stub"));
        assertEquals(0, registry.size());
    }

    static class StubPlugin implements AppPlugin {
        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata("stub", "stub", "1", "stub", PluginType.CUSTOM);
        }

        @Override
        public void init(PluginContext context) {
            // no-op
        }

        @Override
        public PluginResult<?> execute(PluginRequest request) {
            return PluginResult.success("ok");
        }

        @Override
        public void destroy() {
            // no-op
        }
    }
}
