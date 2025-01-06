package com.lc.oj.message;

import com.lc.oj.service.IJudgeService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class MessageConsumer {

    @Resource
    private IJudgeService judgeService;

    @Async("judgeExecutor")
    @RabbitListener(queues = "judgeQueue", ackMode = "MANUAL")
    public void receiveJudgeMessage(Message message, Channel channel) {
        //查看线程名称
        log.info("receiveJudgeMessage thread name = {}", Thread.currentThread().getName());
        try {
            String messageBody = new String(message.getBody());
            long questionSubmitId = Long.parseLong(messageBody);
            log.info("receiveJudgeMessage questionSubmitId = {}", questionSubmitId);
            judgeService.checkSubmit(questionSubmitId); // 检查提交是否合法，如果不合法会抛异常
            boolean success = judgeService.doJudge(questionSubmitId);
            if (success) {
                // 消息处理成功，手动确认
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                // 判题服务出了问题，拒绝消息，消息会被重新投递
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        } catch (Exception e) {
            log.error("Failed to process message", e);
            // 如果出现异常，说明消息本身出现了问题，拒绝消息，消息会被丢弃
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }
}