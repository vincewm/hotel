package com.vince.hotel.config;

import com.vince.hotel.constant.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {
    @Bean
    public TopicExchange exchange(){
        return new TopicExchange(MqConstants.HOTEL_EXCHANGE,true,false);
    }
    @Bean
    public Queue insertQueue(){
        return new Queue(MqConstants.HOTEL_INSERT_QUEUE,true);
    }
    @Bean
    public Queue deleteQueue(){
        return new Queue(MqConstants.HOTEL_DELETE_QUEUE);   //持久化默认是true
    }
    @Bean
    public Binding insertBinding(){
        return BindingBuilder.bind(insertQueue()).to(exchange()).with(MqConstants.HOTEL_INSERT_KEY);
    }
    @Bean
    public Binding deleteBinding(){
        return BindingBuilder.bind(deleteQueue()).to(exchange()).with(MqConstants.HOTEL_DELETE_KEY);
    }
}
