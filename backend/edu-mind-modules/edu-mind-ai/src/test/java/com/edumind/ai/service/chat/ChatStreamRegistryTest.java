package com.edumind.ai.service.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatStreamRegistryTest {

    private final ChatStreamRegistry registry = new ChatStreamRegistry();

    @Test
    void cancelStopsFlag() {
        String streamId = registry.register();
        assertFalse(registry.isCancelled(streamId));
        registry.cancel(streamId);
        assertTrue(registry.isCancelled(streamId));
        registry.remove(streamId);
    }
}
