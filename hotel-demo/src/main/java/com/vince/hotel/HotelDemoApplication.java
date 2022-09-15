package com.vince.hotel;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.vince.hotel.mapper")
@Slf4j
public class HotelDemoApplication {
    @Bean
    public RestHighLevelClient client(){
        return new RestHighLevelClient(RestClient.builder(HttpHost.create("http://192.168.200.131:9200")));
    }
    public static void main(String[] args) {
        SpringApplication.run(HotelDemoApplication.class, args);
        System.out.println("引导类已启动");
    }

}
