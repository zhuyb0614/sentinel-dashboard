package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaDegradeRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaRuleRepositoryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 降级
 */
@Service
public class DegradeService extends BatchRuleService<DegradeRuleEntity> {

    private final Logger logger = LoggerFactory.getLogger(BatchRuleService.class);

    @Autowired
    private JpaDegradeRuleStore jpaRuleStore;
    @Autowired
    private JpaRuleRepositoryAdapter<DegradeRuleEntity> repository;

    @PostConstruct
    public void init() {
        repository.setJpaRuleStore(jpaRuleStore);
    }

    @Autowired
    private SentinelApiClient sentinelApiClient;

    @Override
    public boolean publishRules(String app, String ip, Integer port) {
        List<DegradeRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
        return sentinelApiClient.setDegradeRuleOfMachine(app, ip, port, rules);
    }

    @Override
    protected Result<DegradeRuleEntity> addSingleMachineRule(DegradeRuleEntity degradeRuleEntity, MachineInfo machine) {
        DegradeRuleEntity entity = new DegradeRuleEntity();
        BeanUtils.copyProperties(degradeRuleEntity, entity);
        entity.setId(null);
        entity.setApp(machine.getApp());
        entity.setIp(machine.getIp());
        entity.setPort(machine.getPort());
        try {
            repository.save(entity);
        } catch (Throwable throwable) {
            logger.error("add error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
            logger.info("publish degrade rules fail after rule add");
        }
        return Result.ofSuccess(entity);
    }

    @Override
    protected Result<Long> deleteSingleMachineRule(DegradeRuleEntity rule) {
        try {
            repository.delete(rule.getId());
        } catch (Throwable throwable) {
            logger.error("delete error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        if (!publishRules(rule.getApp(), rule.getIp(), rule.getPort())) {
            logger.info("publish degrade rules fail after rule delete");
        }
        return Result.ofSuccess(rule.getId());
    }

    @Override
    public JpaRuleRepositoryAdapter<DegradeRuleEntity> getRepository() {
        return repository;
    }
}
