package com.picseek.core;

import com.picseek.core.common.config.OCRConfig;
import com.picseek.core.common.config.OSSConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableRetry
@SpringBootApplication
@EnableConfigurationProperties({OSSConfig.class, OCRConfig.class})
public class PicSeekApplication {

    public static void main(String[] args) {
        SpringApplication.run(PicSeekApplication.class, args);
    }

}
