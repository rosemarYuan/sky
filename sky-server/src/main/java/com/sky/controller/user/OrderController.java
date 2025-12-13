package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "用户端口订单相关接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderservice;

    /**
     * 用户下单接口
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单接口")
    public Result<OrderSubmitVO> submit(@RequestBody  OrdersSubmitDTO ordersSubmitDTO) {

        log.info("用户下单，{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO= orderservice.submitOrder(ordersSubmitDTO);

        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderservice.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 分页查询历史订单
     * 接口文档输入为Query参数，即参数为拼接在URL后字符串，记住就行。
     * 此时不用@RequestBody注解
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("分页查询历史订单")
    private Result<PageResult> page(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("分页查询历史订单：页数为{}，每页{}条记录，订单状态为{}", ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize(),
                ordersPageQueryDTO.getStatus());
        PageResult pageResult = orderservice.pageQuery4User(ordersPageQueryDTO);
        return Result.success(pageResult);

    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详情")
    public Result<OrderVO> detail(@PathVariable Long id) {
        log.info("查询订单详情：订单id为{}", id);
        OrderVO orderVO = orderservice.detail(id);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param id
     * @return
     * @throws Exception
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable Long id) throws Exception {
        log.info("取消订单：订单id为{}", id);
        orderservice.userCancelById(id);
        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        log.info("再来一单：订单id为{}", id);
        orderservice.repetition(id);
        return Result.success();
    }

    /**
     * 客户催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("客户催单")
    private Result reminder(@PathVariable("id") Long id){
        orderservice.reminder(id);

        return Result.success();
    }



}
