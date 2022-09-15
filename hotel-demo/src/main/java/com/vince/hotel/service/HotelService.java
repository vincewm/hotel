package com.vince.hotel.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vince.hotel.pojo.Hotel;
import com.vince.hotel.pojo.PageResult;
import com.vince.hotel.pojo.RequestParams;

import java.util.List;
import java.util.Map;

public interface HotelService extends IService<Hotel> {
    PageResult search(RequestParams requestParam);

    Map<String, List<String>> filters(RequestParams requestParams);

    List<String> getSuggestion(String prefix);
    void removeEsById(Long id);
    void saveEsById(Long id);
}
