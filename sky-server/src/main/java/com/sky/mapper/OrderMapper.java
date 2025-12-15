package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号和用户id查询订单
     * @param orderNumber
     * @param userId
     */
    @Select("select * from orders where number = #{orderNumber} and user_id= #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id = #{orderId}")
    Orders getById(Long orderId);

    @Update("update orders set status = #{orderStatus},pay_status = #{orderPaidStatus} ,checkout_time = #{check_out_time} where id = #{id}")
    void updateStatus(Integer orderStatus, Integer orderPaidStatus, LocalDateTime check_out_time, Long id);

    @Select("select count(*) from orders where status = #{status}")
    Integer countStatue(Integer status);

    /**
     *  根据订单窗台和下单时间查询订单
     * @param status
     * @param check_out_time
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{check_out_time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime check_out_time);

    /**
     *根据条件动态添加数据
     * @param map
     * @return
     */
    Double sumByMap(Map<String, Object> map);

    /**
     * 根据动态条件订单数据统计
     * @param map
     * @return
     */
    Integer countByMap(Map<String, Object> map);

    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
