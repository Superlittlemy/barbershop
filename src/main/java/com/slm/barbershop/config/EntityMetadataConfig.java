package com.slm.barbershop.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.slm.barbershop.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 实体元数据处理器
 */
@Component
public class EntityMetadataConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = UserContext.getUser() != null ? UserContext.getUser().getId() : null;
        this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "version", Long.class, 0L);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = UserContext.getUser() != null ? UserContext.getUser().getId() : null;
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, userId);
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime.class, LocalDateTime.now());
    }

}
