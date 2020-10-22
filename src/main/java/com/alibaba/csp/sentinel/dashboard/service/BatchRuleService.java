package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.BatchRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaRuleRepositoryAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public abstract class BatchRuleService<T extends BatchRuleEntity> {
    private @Autowired
    AppManagement appManagement;

    /**
     * 发布所有规则到机器
     *
     * @param machineInfo
     * @return
     */
    public boolean publishRules(MachineInfo machineInfo, boolean isNewMachine) {
        if (isNewMachine) {
            machineRules(machineInfo);
        }
        return publishRules(machineInfo.getApp(), machineInfo.getIp(), machineInfo.getPort());
    }

    /**
     * 获取机器所有规则,如果是新机器,将自动复制第一台机器规则到自身
     *
     * @param machineInfo
     * @return
     */
    public List<T> machineRules(MachineInfo machineInfo) {
        List<T> rules = getRepository().findAllByMachine(machineInfo);
        String app = machineInfo.getApp();
        if (CollectionUtils.isEmpty(rules)) {
            List<T> appRules = getRepository().findAllByApp(app);
            if (!CollectionUtils.isEmpty(appRules)) {
                T firstRule = appRules.get(0);
                for (T appRule : appRules) {
                    if (firstRule.getIp().equals(appRule.getIp()) && firstRule.getPort().equals(appRule.getPort())) {
                        addSingleMachineRule(appRule, machineInfo);
                    }
                }
            }
            rules = getRepository().findAllByMachine(machineInfo);
        }
        return rules;
    }

    /**
     * 添加规则到所有机器
     *
     * @param entity
     * @return
     */
    public Result<T> addAllMachineRule(T entity) {
        AppInfo appInfo = appManagement.getDetailApp(entity.getApp());
        //获取该app所有machine
        Set<MachineInfo> machines = appInfo.getMachines();
        //给添加数据增加一个相同的parentId以供删除,修改
        String parentId = UUID.randomUUID().toString();
        String errorMsg = "";
        for (MachineInfo machine : machines) {
            entity.setParentId(parentId);
            Result<T> e = addSingleMachineRule(entity, machine);
            if (e != null && !e.isSuccess()) {
                errorMsg += e.getMsg();
            }
        }
        if (!"".equals(errorMsg)) {
            return Result.ofFail(-1, errorMsg);
        } else {
            return Result.ofSuccess(entity);
        }
    }

    /**
     * 删除所有规则到机器
     *
     * @param rules
     * @return
     */
    @Transactional
    public Result<Long> deleteAllMachineRule(List<T> rules) {
        String errorMsg = "";
        for (T rule : rules) {
            Result<Long> e = deleteSingleMachineRule(rule);
            if (e != null && !e.isSuccess()) {
                errorMsg += e.getMsg();
            }
        }
        if (!"".equals(errorMsg)) {
            return Result.ofFail(-1, errorMsg);
        } else {
            return Result.ofSuccess(null);
        }
    }


    protected abstract Result<T> addSingleMachineRule(T entity, MachineInfo machine);

    protected abstract Result<Long> deleteSingleMachineRule(T rule);

    public abstract JpaRuleRepositoryAdapter<T> getRepository();

    protected abstract boolean publishRules(String app, String ip, Integer port);
}
