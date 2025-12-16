package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService;

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
     * 导出运营数据报表
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. 查询数据库，获取最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 查询概览数据（这是Service里封装好的方法，查询某段时间内的 营业额、订单完成率等）
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        // 2. 通过输入流读取 Excel 模板文件
        // 路径基于 classpath，对应 src/main/resources/template/运营数据报表模板.xlsx
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 3. 基于模板文件创建一个新的 Excel 对象 (Workbook)
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 4. 获取第一个页签 (Sheet1)
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // ------------------ 填充概览数据 (表格上方) ------------------

            // 填充时间范围：第2行，第2列 (索引从0开始，Row 1, Cell 1)
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            // 获得第4行 (Row 3)
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());       // 第3列：营业额
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate()); // 第5列：订单完成率
            row.getCell(6).setCellValue(businessData.getNewUsers());       // 第7列：新增用户数

            // 获得第5行 (Row 4)
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount()); // 第3列：有效订单
            row.getCell(4).setCellValue(businessData.getUnitPrice());       // 第5列：平均客单价

            // ------------------ 填充明细数据 (表格下方列表) ------------------

            // 5. 循环查询每一天的数据，并填入下方表格
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 查询某一天的详细数据
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                // 从第8行开始往下填 (Row 7)
                row = sheet.getRow(7 + i);
                if (row == null) {
                    // 如果模板里行不够，就创建一个新行
                    row = sheet.createRow(7 + i);
                }

                row.getCell(1).setCellValue(date.toString());            // 日期
                row.getCell(2).setCellValue(data.getTurnover());         // 营业额
                row.getCell(3).setCellValue(data.getValidOrderCount());  // 有效订单
                row.getCell(4).setCellValue(data.getOrderCompletionRate()); // 完成率
                row.getCell(5).setCellValue(data.getUnitPrice());        // 平均客单价
                row.getCell(6).setCellValue(data.getNewUsers());         // 新增用户
            }

            // ------------------ 6. 输出文件 ------------------

            // 通过输出流将 Excel 文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
