package pk.la.pasir_lech_artur.websocket;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.la.pasir_lech_artur.dto.GroupExpenseNotificationDTO;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupNotificationWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, Set<WebSocketSession>> sessionsByUserId = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        Long userId = getUserIdFromSession(session);

        if (userId != null) {
            sessionsByUserId
                    .computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet())
                    .add(session);
        }
    }

    @Override
    public void afterConnectionClosed(
            @NonNull WebSocketSession session,
            @NonNull CloseStatus status) {

        Long userId = getUserIdFromSession(session);

        if (userId != null) {
            Set<WebSocketSession> sessions = sessionsByUserId.get(userId);

            if (sessions != null) {
                sessions.remove(session);

                if (sessions.isEmpty()) {
                    sessionsByUserId.remove(userId);
                }
            }
        }
    }

    public void sendNotificationToUser(Long userId, GroupExpenseNotificationDTO notification) {
        Set<WebSocketSession> sessions = sessionsByUserId.get(userId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json = toJson(notification);
        TextMessage message = new TextMessage(json);

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    throw new RuntimeException("Nie udało się wysłać powiadomienia WebSocket.", e);
                }
            }
        }
    }

    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");

        if (userId instanceof Long) {
            return (Long) userId;
        }

        return null;
    }

    private String toJson(GroupExpenseNotificationDTO notification) {
        return "{"
                + "\"type\":\"" + escape(notification.getType()) + "\","
                + "\"groupId\":" + notification.getGroupId() + ","
                + "\"groupName\":\"" + escape(notification.getGroupName()) + "\","
                + "\"title\":\"" + escape(notification.getTitle()) + "\","
                + "\"amount\":" + notification.getAmount() + ","
                + "\"userShare\":" + notification.getUserShare() + ","
                + "\"createdByEmail\":\"" + escape(notification.getCreatedByEmail()) + "\","
                + "\"message\":\"" + escape(notification.getMessage()) + "\""
                + "}";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}