package com.vince.hotel.mq;

import com.vince.hotel.constant.MqConstants;
import com.vince.hotel.service.HotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HotelListener {
    @Autowired
    private HotelService hotelService;
    @RabbitListener(queues = MqConstants.HOTEL_INSERT_QUEUE)
    public void listenHotelEsCreateAndUpdate(Long id){
        log.info("监听到保存消息");
        hotelService.saveEsById(id);
    }
    @RabbitListener(queues = MqConstants.HOTEL_DELETE_QUEUE)
    public void listenHotelEsDelete(Long id){
        hotelService.removeEsById(id);
    }

}
