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
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1OTE1MTE2NDF9.c24RU6_3JJYcQvG3CXLiynrWZ7nZz1kPvc22uNzBpDonGp0177pC2mLafewVOXz38pZHINtODgJ8ItqcBIQu1Dp2ltLy56PAmHAbE-Y2yCp3C-G3nte71Hwbyk4gW2H-0R2XOvcTzmlTGqyZ1V5G00_LVIosDa8UdYaWxx9Qis-K3YD7IHe8onx_GdpbFLgPrtaUDVUL15EkmFYNZRnzRVa4M8W9qZDeBqpCXAIhe8lA5CzmmENUrx0xQXNQsnH97N2cTDWlf3TlKFfhjp2XadTozKdlCgDr9qYuECGSCB8sUiZRtn5tPnFEJfvI7j2_DetxT_ScXlKKwJwoi8DRnw";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}
