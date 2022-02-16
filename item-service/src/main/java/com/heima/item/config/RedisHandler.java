package com.heima.item.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heima.item.pojo.Item;
import com.heima.item.pojo.ItemStock;
import com.heima.item.service.IItemService;
import com.heima.item.service.IItemStockService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisHandler implements InitializingBean {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IItemService itemService;

    @Autowired
    private IItemStockService itemStockService;

    @Override
    public void afterPropertiesSet() throws Exception {
//        初始化缓存
        List<Item> itemList = itemService.list();
//        1.查询商品信息
        for (Item item : itemList) {
//            2.1 item序列化为JSON
            String json = MAPPER.writeValueAsString(item);
//            2.2存入redis
            redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        }

        //        2.查询商品库存信息
        List<ItemStock> itemStocks = itemStockService.list();
        for (ItemStock stock : itemStocks) {
//            2.1 item序列化为JSON
            String json = MAPPER.writeValueAsString(stock);
//            2.2存入redis
            redisTemplate.opsForValue().set("item:stock:id:" + stock.getId(), json);
        }
    }

    public void saveItem(Item item) {
        try {
            String json = MAPPER.writeValueAsString(item);
            redisTemplate.opsForValue().set("item:id:" + item.getId(), json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteItemById(Long id) {
        redisTemplate.delete("item:id:" + id);
    }
}
