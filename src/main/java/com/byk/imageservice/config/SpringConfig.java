package com.byk.imageservice.config;

import com.amazonaws.services.s3.model.Bucket;
import com.byk.imageservice.aws.AWSS3Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SpringConfig {
    @Bean
    public AWSS3Client awsS3Client() {
        return new AWSS3Client();
    }

    @Bean
    public Bucket createBucket() {
        String bucketName = "byk-images";
        return awsS3Client().createBucket(bucketName);
    }
}
