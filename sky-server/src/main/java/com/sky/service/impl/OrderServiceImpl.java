package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // --- A. 前期准备——业务异常校验 [地址]、[购物车]是否为空 --- 
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook==null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList==null || shoppingCartList.size()==0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // --- B. 向订单表插入一条数据 ---
        // 构造数据，注意自己写时属性不能漏
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);

        orders.setOrderTime(LocalDateTime.now());
        orders.setPayMethod(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));

        orders.setAddress(addressBook.getProvinceName()
                +addressBook.getCityName()
                +addressBook.getDistrictName()
                +addressBook.getDetail());
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        // 计算总金额
        BigDecimal totalAmount = new BigDecimal(0);
        for (ShoppingCart cart : shoppingCartList) {
            // 单价 * 数量
            BigDecimal itemAmount = cart.getAmount().multiply(new BigDecimal(cart.getNumber()));
            totalAmount = totalAmount.add(itemAmount);
        }
        orders.setAmount(totalAmount);

        orderMapper.insert(orders);

        // --- C. 向订单明细表插入多条数据 (order_detail) ---

        List<OrderDetail> orderDetailList = new ArrayList<>();
        // 6. 遍历购物车，转为订单明细
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail); // 拷贝：名字、图片、价格、口味、数量
            orderDetail.setOrderId(orders.getId());// 设置关联的订单ID (这是上一步 insert 之后回显回来的)
            orderDetailList.add(orderDetail);
        }
        // 7. 批量插入订单明细 (性能优化：不要在循环里调 insert)
        orderDetailMapper.insertBatch(orderDetailList);
        // 8. 清空购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        // 9. 封装返回结果VO
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }
}
