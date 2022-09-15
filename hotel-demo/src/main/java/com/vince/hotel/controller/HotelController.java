package com.vince.hotel.controller;

import com.vince.hotel.pojo.PageResult;
import com.vince.hotel.pojo.RequestParams;
import com.vince.hotel.service.HotelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
@Slf4j
public class HotelController {
    @Autowired
    private HotelService hotelService;
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams requestParams){

        return hotelService.search(requestParams);
    }
    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody RequestParams requestParams){

        return hotelService.filters(requestParams);
    }
    @GetMapping("/suggestion")
    public List<String> getSuggestion(@RequestParam("key") String prefix){
        return hotelService.getSuggestion(prefix);
    }
}
