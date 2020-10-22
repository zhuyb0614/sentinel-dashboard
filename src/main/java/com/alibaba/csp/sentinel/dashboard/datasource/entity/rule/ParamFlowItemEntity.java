package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;

/**
 * @author zhuyb
 * @date 10/22/2020 11:49 AM
 * 热点参数附加
 */
@Table
@Entity
public class ParamFlowItemEntity {
    private Long id;
    private Long ruleId;
    private String object;
    private Integer count;
    private String classType;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public ParamFlowItem toItem() {
        ParamFlowItem paramFlowItem = new ParamFlowItem();
        BeanUtils.copyProperties(this, paramFlowItem);
        return paramFlowItem;
    }

    public static ParamFlowItemEntity fromItem(ParamFlowItem paramFlowItem) {
        ParamFlowItemEntity paramFlowItemEntity = new ParamFlowItemEntity();
        BeanUtils.copyProperties(paramFlowItem, paramFlowItemEntity);
        return paramFlowItemEntity;
    }
}
