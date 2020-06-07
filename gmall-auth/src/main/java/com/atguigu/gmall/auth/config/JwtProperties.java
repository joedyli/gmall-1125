package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * 专门读取jwt配置信息
 */
@ConfigurationProperties(prefix = "jwt")
@Data
@Slf4j
public class JwtProperties {

    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    /**
     * 读取公钥和私钥文件对应的内容到公私钥对象
     * 将来就可以直接使用公私钥对象，不需要每次去读取文件了
     */
    @PostConstruct
    public void init(){
        try {
            File publicFile = new File(pubKeyPath);
            File privateFile = new File(priKeyPath);
            if (!publicFile.exists() || !privateFile.exists()){
                // 如果公钥文件或者是私钥文件不存在，则重新生成
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
            // 初始化公私钥对象
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("生成公钥和私钥异常！" + e.getMessage());
        }
    }
}
