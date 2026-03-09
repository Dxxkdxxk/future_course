package com.lzlz.springboot.security.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component // 将这个类声明为 Spring Bean
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 在执行插入操作时调用
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 参数说明：
        // 1. 实体类中的字段名（驼峰命名）
        // 2. 要填充的值
        // 3. metaObject
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

    /**
     * 在执行更新操作时调用
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 同样，更新时只需要填充 updateTime
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
