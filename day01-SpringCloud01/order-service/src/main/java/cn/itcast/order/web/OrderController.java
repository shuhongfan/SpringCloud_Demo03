package cn.itcast.order.web;

import cn.itcast.order.pojo.Order;
import cn.itcast.order.service.OrderService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @SentinelResource("hot")
    @GetMapping("{orderId}")
    public Order queryOrderByUserId(@PathVariable("orderId") Long orderId,
                                    @RequestHeader(value = "truth", required = false) String truth) {
        System.out.println("truth:" + truth);
        // 根据id查询订单并返回
        return orderService.queryOrderById(orderId);
    }

    @GetMapping("/query")
    public String queryOrder() {
        orderService.queryGoods();
        System.out.println("查询订单");
        return "查询成功";
    }

    @GetMapping("/save")
    public String saveOrder() {
        orderService.queryGoods();
        System.out.println("新增订单");
        return "新增订单成功";
    }

    @GetMapping("/update")
    public String updateOrder() {
        return "更新订单成功";
    }
}
