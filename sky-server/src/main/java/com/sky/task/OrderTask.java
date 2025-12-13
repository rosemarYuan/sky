package com.sky.task;

import com.fasterxml.jackson.databind.util.LookupCache;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder(){
        log.info("定时处理超时订单， {}", LocalDateTime.now());

        // select * from orders where status = ? and order_time + 15  < LD.now()
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(
                Orders.PENDING_PAYMENT,
                LocalDateTime.now().plusMinutes(-15));

        if(ordersList != null && ordersList.size() > 0 ){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }

    }

    /**
     * 每日自动完成一直在派送住状态中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDelivery(){
        log.info("自动处理还处于派送状态下的订单 {}", LocalDateTime.now());

        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(
                Orders.DELIVERY_IN_PROGRESS,
                LocalDateTime.now().plusHours(-1)
        );

        if(ordersList != null && ordersList.size() > 0 ){
            for(Orders orders : ordersList){
                orders.setStatus(Orders.COMPLETED);
                orders.setCancelReason("订单自动确认");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
