package com.labOS.backend.satoken;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 读取项目相关配置
 * 
 * @author <a href="https://github.com/Dannywen1213dup">Yifan Wen</a>
 * @from <a href="https://www.ai4labos.com/">ai4labOS</a>
 */
@Data
@Component
@ConfigurationProperties(prefix = "labos")
public class V2Config {
    
    /** 项目名称 */
    private String name = "labOS";
    
    /** 版本 */
    private String version = "1.0.0";
    
    /** Sa-Token 不拦截的URL配置 */
    private List<String> saTokenNotFilterUrl = new ArrayList<>();
}
