package pk.la.pasir_lech_artur.websocket;

import io.jsonwebtoken.Claims;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import pk.la.pasir_lech_artur.security.JwtUtil;

import java.util.Map;

@Component
public class GroupNotificationHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    public GroupNotificationHandshakeInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (token == null || token.isBlank() || !jwtUtil.validateToken(token)) {
            return false;
        }

        Claims claims = jwtUtil.extractAllClaims(token);
        Object userIdClaim = claims.get("id");
        Long userId = toLong(userIdClaim);

        if (userId == null) {
            return false;
        }

        attributes.put("userId", userId);
        attributes.put("email", claims.getSubject());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }
}