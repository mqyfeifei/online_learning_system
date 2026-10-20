package com.mqy.olearning;

import com.mqy.olearning.utils.Md5Util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加盐工具单元测试
 */
class Md5UtilTest {

    @Test
    @DisplayName("同样的密码 + 同样的盐，结果必须一致")
    void encryptIsStable() {
        String a = Md5Util.encrypt("123456", "a1b2c3d4");
        String b = Md5Util.encrypt("123456", "a1b2c3d4");
        assertEquals(a, b);
        assertEquals("416a903d7521048c970c8388821b30c6", a, "应能对上初始化脚本里的哈希");
    }

    @Test
    @DisplayName("同样的密码 + 不同的盐，结果必须不同（防彩虹表）")
    void differentSaltDifferentHash() {
        String a = Md5Util.encrypt("123456", "a1b2c3d4");
        String b = Md5Util.encrypt("123456", "ffffffff");
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("随机盐长度固定为 8")
    void randomSaltLength() {
        assertEquals(8, Md5Util.randomSalt().length());
    }
}
