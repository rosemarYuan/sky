package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    // TODO: 【高并发优化】升级为 "Redis分布式锁 + 数据库唯一索引" 双重保障模式
    // 当前现状：仅依赖数据库唯一索引抛出异常来保证唯一性，高并发下数据库写压力大。
    // 优化方案：
    // 1. 第一道防线（Redis）：引入 Redisson 分布式锁。
    //    - 作用：在内存层面拦截 99.9% 的并发重复请求，避免无效流量打到数据库。
    //    - Key设计：lock:employee:register:{username}
    // 2. 第二道防线（MySQL）：保留当前的数据库唯一索引 (Unique Index)。
    //    - 作用：作为最终兜底机制（The Safety Net）。
    //    - 场景：应对 Redis 集群宕机、锁意外过期等极端 0.1% 的情况，确保数据绝对一致。

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        // Duplicate entry 'aini' for key 'employee.idx_username'
        // 判断返回的异常信息中有没有前面的关键字Duplicate entry，有的话返回重复的人名（下一个字符）
        String message = ex.getMessage();
        if(message.contains("Duplicate entry")) {
            message = message.split(" ")[2].replace("'", "");
            message = "异常信息: name" + message + MessageConstant.ALREADY_EXISTS;
            log.error("异常信息：{} 已经存在", message);
            return Result.error(message);
        }else{
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }

    }

}
