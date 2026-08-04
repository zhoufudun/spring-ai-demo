package com.git.hui.springai.advance.config;

import com.github.microwww.redis.RedisServer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;


@Configuration
public class RedisConfig {
    @Bean
    public RedisServer mockRedisServer(RedisProperties redisProperties) throws IOException {
        RedisServer server = new RedisServer();
        server.listener(redisProperties.getHost(), redisProperties.getPort());
        return server;
    }

}
