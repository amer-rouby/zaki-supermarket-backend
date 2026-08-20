package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationStreamService {

    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByStore = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long storeId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByStore.computeIfAbsent(storeId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(storeId, emitter));
        emitter.onTimeout(() -> removeEmitter(storeId, emitter));
        emitter.onError(error -> removeEmitter(storeId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            removeEmitter(storeId, emitter);
        }

        return emitter;
    }

    public void notifyCreated(Long storeId, NotificationResponse notification) {
        if (storeId == null || notification == null) {
            return;
        }
        sendEvent(storeId, "notification-created", notification);
    }

    public void notifyChanged(Long storeId) {
        if (storeId == null) {
            return;
        }
        sendEvent(storeId, "notifications-changed", Map.of("refresh", true));
    }

    private void sendEvent(Long storeId, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByStore.get(storeId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(storeId, emitter);
            }
        }
    }

    private void removeEmitter(Long storeId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByStore.get(storeId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByStore.remove(storeId);
        }
    }
}
