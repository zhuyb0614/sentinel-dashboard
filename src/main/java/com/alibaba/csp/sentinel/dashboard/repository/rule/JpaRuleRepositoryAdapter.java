package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.taodangpu.sentinel.exclude.JpaRuleStore;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Scope("prototype")
public class JpaRuleRepositoryAdapter<T extends RuleEntity> implements RuleRepository<T, Long> {

    private JpaRuleStore<T> jpaRuleStore;

    public void setJpaRuleStore(JpaRuleStore jpaRuleStore) {
        this.jpaRuleStore = jpaRuleStore;
    }

    @Override
    public T save(T entity) {
        entity.preInsert();
        return jpaRuleStore.save(entity);
    }

    @Override
    public List<T> saveAll(List<T> rules) {
        rules.forEach(RuleEntity::preInsert);
        return jpaRuleStore.saveAll(rules);
    }

    @Override
    public T delete(Long aLong) {
        Optional<T> freOpt = jpaRuleStore.findById(aLong);
        if (freOpt.isPresent()) {
            T t = freOpt.get();
            jpaRuleStore.delete(t);
            return t;
        }
        return null;
    }

    @Override
    public T findById(Long aLong) {
        Optional<T> freOpt = jpaRuleStore.findById(aLong);
        if (freOpt.isPresent()) {
            T T = freOpt.get();
            return T;
        }
        return null;
    }

    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        return jpaRuleStore.findByMachine(machineInfo.getApp(), machineInfo.getIp(), machineInfo.getPort());
    }

    @Override
    public List<T> findAllByApp(String appName) {
        return jpaRuleStore.findByApp(appName);
    }

    public List<T> findByParentId(String parentId) {
        return jpaRuleStore.findByParentId(parentId);
    }
}
