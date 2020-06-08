package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\project-1125\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\project-1125\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "@343sdfljsd^^(#DSD234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTE1ODYwMzl9.NhBykhp1ue94u4vR7-uzQEq-kbrd3Yx-8y-qZYJzaOq-fKK34ggWLDXBP5ffdOVsu8MszmN7wpTlvWOH06cRNG1mTTRTEYtq7mOfWbHFCOgqvIROhLELCKBsWdB5C6TRcAWYMdE85ZQsDIQzrZRNWv93_2LyZUJS6DNBfYzar-vryagRat4-sKm-bsv6Zn3bsPoa-YdL9lO67xGmBdhLQpkLTdIZhed5WZ7PBHuItC1s7flqkTVnLdmSQprJmri0wn50GC_npzdxB14Xjf2CaVdOqGMbdmIrOCzJx2BlJhffLmsfOmA6HwdYNtWeQ3QUoLX5fmCoxe1XDF08UDfFyg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
