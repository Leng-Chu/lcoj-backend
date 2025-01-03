package com.lc.oj.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendJudgeMessage(Long questionSubmitId) {
        log.info("sendJudgeMessage questionSubmitId = {}", questionSubmitId);
        rabbitTemplate.convertAndSend("judgeExchange", "judge", questionSubmitId.toString());
    }
}