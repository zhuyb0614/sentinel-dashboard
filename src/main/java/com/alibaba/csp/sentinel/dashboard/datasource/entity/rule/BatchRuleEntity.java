package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

public interface BatchRuleEntity extends RuleEntity {
    String getParentId();

    void setParentId(String parentId);
}
