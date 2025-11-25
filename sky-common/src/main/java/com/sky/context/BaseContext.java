package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 存入线程局部变量
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    // 获得线程中的局部变量
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    // 删除线程中的局部变量
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
