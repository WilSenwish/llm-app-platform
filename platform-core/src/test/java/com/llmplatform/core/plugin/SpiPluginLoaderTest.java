package com.llmplatform.core.plugin;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpiPluginLoaderTest {

    @Test
    void loadsPluginDeclaredInServiceFile() {
        SpiPluginLoader loader = new SpiPluginLoader();

        boolean found = loader.load().stream()
                .anyMatch(plugin -> "sample-spi".equals(plugin.getMetadata().id()));

        assertTrue(found);
    }
}
