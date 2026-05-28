package com.signagetv.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Publica eventos en tiempo real por WebSocket para que las TVs se resincronicen.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /** Notifica a todas las TVs de un local que algo cambió y deben recargar. */
    public void notifyPlaylistsChanged(Long localId) {
        Map<String, Object> payload = Map.of(
                "type", "REFRESH",
                "localId", localId,
                "timestamp", Instant.now().toString()
        );
        String topic = "/topic/local/" + localId + "/playlists";
        messagingTemplate.convertAndSend(topic, payload);
        log.debug("WS publish -> {} : {}", topic, payload);
    }

    /** Envía un comando directo a una TV específica. */
    public void sendTvCommand(Long tvId, String command, Map<String, Object> extra) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("type", command);
        payload.put("tvId", tvId);
        payload.put("timestamp", Instant.now().toString());
        if (extra != null) payload.putAll(extra);
        messagingTemplate.convertAndSend("/topic/tv/" + tvId + "/command", payload);
    }
}
