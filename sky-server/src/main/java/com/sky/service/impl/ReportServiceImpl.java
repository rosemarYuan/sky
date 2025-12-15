package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 提取时间列表
        List<LocalDate> dateList = getLocalDates(begin, end);

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 营业额是状态为已完成状态的订单合计
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED); // 状态为5
            Double turnover = orderMapper.sumByMap(map);
            // select sum(amount) from orders where order_time > ? and order_time < ? and status = 5
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ','))
                .turnoverList(StringUtils.join(turnoverList, ','))
                .build();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUSerStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表 (X)
        List<LocalDate> dateList = getLocalDates(begin, end);

        // 每日的"新增用户数"和"总用户数" (Y)
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        // 3. 遍历日期，根据
        // select count(id) from user where create_time< ?  and create_time > ?
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 当天 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);   // 当天 23:59:59

            // --- 查询总用户数 (select count(id) from user where create_time < endTime) ---
            Map<String, Object> map = new HashMap<>();
            map.put("end", endTime);
            // 只要是在今天结束之前注册的，都算进总数
            Integer totalUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);

            // --- 查询新增用户数 (select count(id) from user where create_time > beginTime and create_time < endTime) ---
            map.put("begin", beginTime);
            // 在今天 0点 到 23点59分 之间注册的，算新增
            Integer newUser = userMapper.countByMap(map);
            newUserList.add(newUser);
        }

        // 4. 封装 VO 结果并返回
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))      // List -> "2023-10-01,2023-10-02"
                .totalUserList(StringUtils.join(totalUserList, ",")) // List -> "100,105,110"
                .newUserList(StringUtils.join(newUserList, ","))     // List -> "5,5,5"
                .build();
    }

    /**
     * 指定时间内订单数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 生成日期列表 (X)
        List<LocalDate> dateList = getLocalDates(begin, end);

        // 每日的"有效订单数"和"总订单数" (Y)
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // 当天 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);   // 当天 23:59:59

            // 准备查询条件 Map
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);

            // --- 查询当天的【订单总数】 ---
            // SQL: select count(id) from orders where order_time > ? and order_time < ?
            Integer orderCount = orderMapper.countByMap(map);
            orderCountList.add(orderCount);

            // --- 查询当天的【有效订单数】 ---
            // 有效订单 = 状态为“已完成”(Orders.COMPLETED = 5)
            // SQL: select count(id) from orders where order_time > ? and order_time < ? and status = 5
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countByMap(map);
            validOrderCountList.add(validOrderCount);
        }

        // 4. 计算时间区间内的汇总数据 (VO 需要的额外字段)
        // 订单总数 (累加 orderCountList)
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        // 有效订单数 (累加 validOrderCountList)
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);

        // 订单完成率 = 有效订单数 / 订单总数
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        // 5. 封装 VO 结果并返回
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))      // 每日订单总数列表
                .validOrderCountList(StringUtils.join(validOrderCountList, ",")) // 每日有效订单列表
                .totalOrderCount(totalOrderCount)     // 区间总订单数
                .validOrderCount(validOrderCount)     // 区间有效订单数
                .orderCompletionRate(orderCompletionRate) // 订单完成率
                .build();
    }

    /**
     * 销量排名前10
     */
    @Override
    public SalesTop10ReportVO getTop10Statistics(LocalDate begin, LocalDate end) {
        // 1. 转换时间格式 (LocalDate -> LocalDateTime)
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 2. 调用 Mapper 查询数据库,返回DTO，包含 name 和 number 两个属性
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = goodsSalesDTOList.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.toList());

        List<Integer> numbers = goodsSalesDTOList.stream()
                .map(GoodsSalesDTO::getNumber)
                .collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))       // "鱼香肉丝,宫保鸡丁,米饭"
                .numberList(StringUtils.join(numbers, ","))   // "200,150,100"
                .build();
    }

    /**
     * 提取时间列表方法
     * @param begin
     * @param end
     * @return
     */
    private static List<LocalDate> getLocalDates(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        return dateList;
    }
}
