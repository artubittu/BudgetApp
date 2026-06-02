package pk.la.pasir_lech_artur.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import pk.la.pasir_lech_artur.websocket.GroupNotificationHandshakeInterceptor;
import pk.la.pasir_lech_artur.websocket.GroupNotificationWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupNotificationWebSocketHandler groupNotificationWebSocketHandler;
    private final GroupNotificationHandshakeInterceptor groupNotificationHandshakeInterceptor;

    public WebSocketConfig(
            GroupNotificationWebSocketHandler groupNotificationWebSocketHandler,
            GroupNotificationHandshakeInterceptor groupNotificationHandshakeInterceptor) {
        this.groupNotificationWebSocketHandler = groupNotificationWebSocketHandler;
        this.groupNotificationHandshakeInterceptor = groupNotificationHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupNotificationWebSocketHandler, "/ws/group-notifications")
                .addInterceptors(groupNotificationHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}