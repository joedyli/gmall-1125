package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

/**
 * 专门读取jwt配置信息
 */
@ConfigurationProperties(prefix = "jwt")
@Data
@Slf4j
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;
    private String userKey;

    private PublicKey publicKey;

    /**
     * 读取公钥和私钥文件对应的内容到公私钥对象
     * 将来就可以直接使用公私钥对象，不需要每次去读取文件了
     */
    @PostConstruct
    public void init(){
        try {
            // 初始化公私钥对象，这里只需要读取公钥，私钥是留给auth自己的。密钥生成是由auth生成
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("读取公钥异常！" + e.getMessage());
        }
    }
}
