package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.BatchRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author zhuyb
 * @date 10/14/2020 5:59 PM
 * 集群批量任务
 */
public abstract class ClusterBatchRuleService<T extends BatchRuleEntity> extends BatchRuleService<T> {
    @Autowired
    protected ClusterConfigService clusterConfigService;
    @Override
    public Result<T> addAllMachineRule(T entity) {
        Result<T> tResult = super.addAllMachineRule(entity);
        publishClusterConfig();
        return tResult;
    }

    @Override
    public Result<Long> deleteAllMachineRule(List<T> rules) {
        Result<Long> longResult = super.deleteAllMachineRule(rules);
        publishClusterConfig();
        return longResult;
    }
    @Override
    public List<T> machineRules(MachineInfo machineInfo) {
        List<T> rules = super.machineRules(machineInfo);
        addClusterConfig(rules);
        addExt(rules);
        return rules;
    }

    protected abstract void addClusterConfig(List<T> rules);
    protected abstract void addExt(List<T> rules);

    public abstract void publishClusterConfig();
}
