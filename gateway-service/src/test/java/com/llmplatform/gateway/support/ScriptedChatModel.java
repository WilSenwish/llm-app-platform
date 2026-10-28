package com.llmplatform.gateway.support;

import com.llmplatform.core.model.ModelUnavailableException;
import com.llmplatform.gateway.model.ChatModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 测试用端点：可以失败、休眠或按增量吐字。 */
public final class ScriptedChatModel implements ChatModel {

    private final String id;
    private final Supplier<String> completeResult;
    private final List<String> deltas;
    private final AtomicInteger completeCalls = new AtomicInteger();
    private volatile long sleepMs;
    private volatile boolean probeFails;

    public ScriptedChatModel(String id, Supplier<String> completeResult, List<String> deltas) {
        this.id = id;
        this.completeResult = completeResult;
        this.deltas = List.copyOf(deltas);
    }

    public ScriptedChatModel sleepMs(long sleepMs) {
        this.sleepMs = sleepMs;
        return this;
    }

    public ScriptedChatModel probeFails(boolean probeFails) {
        this.probeFails = probeFails;
        return this;
    }

    public int completeCalls() {
        return completeCalls.get();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String complete(String prompt) {
        completeCalls.incrementAndGet();
        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ModelUnavailableException(id + " interrupted", ex);
            }
        }
        return completeResult.get();
    }

    @Override
    public void stream(String prompt, Consumer<String> onDelta) {
        String ignored = complete(prompt);
        if (ignored != null) {
            for (String delta : deltas) {
                onDelta.accept(delta);
            }
        }
    }

    @Override
    public void probe() {
        if (probeFails) {
            throw new ModelUnavailableException(id + " probe down");
        }
    }
}
