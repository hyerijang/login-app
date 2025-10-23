package com.koreanmarkers.assignment.login_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koreanmarkers.assignment.login_app.domain.User;
import com.koreanmarkers.assignment.login_app.domain.UserRole;
import com.koreanmarkers.assignment.login_app.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final UserRepository userRepository;
    private final byte[] secretBytes;
    private final long expirationMs;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.expiration-ms}") long expirationMs,
                            UserRepository userRepository) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationMs = expirationMs;
        this.userRepository = userRepository;
    }

    public String generateToken(Long userId, UserRole role) {
        try {
            long nowSec = Instant.now().getEpochSecond();
            long expSec = nowSec + (expirationMs / 1000);

            String headerJson = objectMapper.writeValueAsString(new SimpleMap("alg", "HS256", "typ", "JWT"));
            String payloadJson = objectMapper.writeValueAsString(new SimpleMap(
                    "sub", String.valueOf(userId),
                    "role", role != null ? role.getKey() : null,
                    "iat", nowSec,
                    "exp", expSec
            ));

            String headerB64 = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadB64 = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = headerB64 + "." + payloadB64;
            byte[] signature = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8));
            String sigB64 = base64UrlEncode(signature);

            return signingInput + "." + sigB64;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate token", e);
        }
    }

    public Long getUserIdFromToken(String token) {
        JsonNode payload = parsePayload(token);
        return Long.valueOf(payload.get("sub").asText());
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String signingInput = parts[0] + "." + parts[1];
            byte[] expectedSig = hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] providedSig = base64UrlDecode(parts[2]);
            if (!MessageDigestisEqual(expectedSig, providedSig)) return false;

            JsonNode payload = parsePayload(token);
            long exp = payload.get("exp").asLong();
            long now = Instant.now().getEpochSecond();
            return now < exp;
        } catch (Exception e) {
            return false;
        }
    }

    private JsonNode parsePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT structure");
            byte[] payloadBytes = base64UrlDecode(parts[1]);
            return objectMapper.readTree(payloadBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token payload", e);
        }
    }

    private byte[] hmacSha256(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretBytes, "HmacSHA256");
        mac.init(keySpec);
        return mac.doFinal(data);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    // constant-time comparison
    private static boolean MessageDigestisEqual(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public Authentication getAuthentication(String token) {
        Long userId = getUserIdFromToken(token);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return null;
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getKey());
        return new UsernamePasswordAuthenticationToken(user, token, List.of(authority));
    }

    // tiny helper map for ObjectMapper
    private static class SimpleMap extends java.util.HashMap<String, Object> {
        SimpleMap(Object... kv) {
            for (int i = 0; i + 1 < kv.length; i += 2) {
                put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
    }
}
