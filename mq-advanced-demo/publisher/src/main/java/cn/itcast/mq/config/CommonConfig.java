package cn.itcast.mq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CommonConfig implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//        获取 rabbitTemplate对象
        RabbitTemplate rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
//        配置returnCallback
        rabbitTemplate.setReturnCallback((message, i, s, s1, s2) -> {
//            记录日志
            log.error("消息发送到队列失败,响应码:{},失败原因:{},交换机:{},路由key:{},消息:{}", message, i, s, s1, s2);
//            如果有需要,重发消息
        });
    }
}
