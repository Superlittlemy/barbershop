package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消费项目分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_category")
public class ServiceCategory extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 状态:0.停用 1.启用
     */
    private Integer status;

}
