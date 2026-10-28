package com.llmplatform.core.plugin.spi;

import com.llmplatform.core.plugin.AppPlugin;
import com.llmplatform.core.plugin.PluginContext;
import com.llmplatform.core.plugin.PluginMetadata;
import com.llmplatform.core.plugin.PluginRequest;
import com.llmplatform.core.plugin.PluginResult;
import com.llmplatform.core.plugin.PluginType;

/** 仅用于验证 ServiceLoader 能发现并实例化插件。 */
public class SampleSpiPlugin implements AppPlugin {

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata("sample-spi", "sample", "1", "spi", PluginType.CUSTOM);
    }

    @Override
    public void init(PluginContext context) {
        // no-op
    }

    @Override
    public PluginResult<?> execute(PluginRequest request) {
        return PluginResult.success("spi");
    }

    @Override
    public void destroy() {
        // no-op
    }
}
