package cn.itcast.hotel.web;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;

    @PostMapping("/list")
    public PageResult sarch(@RequestBody RequestParams params) {
        return hotelService.search(params);
    }

    @GetMapping("suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {
        return hotelService.getSuggestion(prefix);
    }

    @PostMapping("/filters")
    public Map<String, List<String>> filters(@RequestBody RequestParams params) {
        return hotelService.filters(params);
    }
}
