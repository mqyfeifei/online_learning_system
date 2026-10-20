package com.mqy.olearning.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 密码工具：MD5(明文 + 盐)
 * 数据库中不存明文，salt 每个用户独立随机
 */
public final class Md5Util {

    private Md5Util() {
    }

    public static String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    public static String encrypt(String rawPassword, String salt) {
        return md5(rawPassword + salt);
    }

    public static String randomSalt() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
