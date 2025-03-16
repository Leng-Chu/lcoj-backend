package com.lc.oj.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendJudgeMessage(Long questionSubmitId) {
        log.info("sendJudgeMessage questionSubmitId = {}", questionSubmitId);
        Message message = MessageBuilder.withBody(questionSubmitId.toString().getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // 设置消息持久化
                .build();
        rabbitTemplate.convertAndSend("judgeExchange", "judge", message);
    }
}