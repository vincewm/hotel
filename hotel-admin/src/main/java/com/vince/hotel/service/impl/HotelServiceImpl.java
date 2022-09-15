package com.vince.hotel.service.impl;

import com.vince.hotel.mapper.HotelMapper;
import com.vince.hotel.pojo.Hotel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vince.hotel.service.HotelService;
import org.springframework.stereotype.Service;

@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements HotelService {
}
