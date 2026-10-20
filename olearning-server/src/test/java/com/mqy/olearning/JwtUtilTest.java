package com.mqy.olearning;

import com.mqy.olearning.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 签发与解析单元测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "online-learning-platform-2026-mqy-secret-key");
        ReflectionTestUtils.setField(jwtUtil, "expire", 7200000L);
    }

    @Test
    @DisplayName("签发的 token 能被解析出 userId 和 role")
    void createAndParse() {
        String token = jwtUtil.createToken(4L, "student1", "STUDENT");
        Claims claims = jwtUtil.parse(token);
        assertEquals("4", claims.getSubject());
        assertEquals("student1", claims.get("username", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
    }

    @Test
    @DisplayName("被篡改的 token 解析必须抛异常")
    void tamperedTokenThrows() {
        String token = jwtUtil.createToken(1L, "admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(Exception.class, () -> jwtUtil.parse(tampered));
    }
}
