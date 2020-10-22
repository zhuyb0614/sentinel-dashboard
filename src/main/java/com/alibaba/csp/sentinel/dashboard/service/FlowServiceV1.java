package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ClusterFlowConfigEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterServerStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterServerStateVO;
import com.alibaba.csp.sentinel.dashboard.repository.rule.ClusterFlowConfigRepository;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.util.ClusterEntityUtils;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 限流
 */
@Service
public class FlowServiceV1 extends ClusterBatchRuleService<FlowRuleEntity> {

    private final Logger logger = LoggerFactory.getLogger(FlowServiceV1.class);

    @Autowired
    private JpaFlowRuleStore jpaRuleStore;
    @Autowired
    private JpaRuleRepositoryAdapter<FlowRuleEntity> repository;

    @PostConstruct
    public void init() {
        repository.setJpaRuleStore(jpaRuleStore);
    }

    @Autowired
    private SentinelApiClient sentinelApiClient;
    @Autowired
    private ClusterFlowConfigRepository clusterFlowConfigRepository;

    /**
     * 设置单台机器规则
     *
     * @param flowRuleEntity
     * @param machine
     * @return
     */
    @Override
    public Result<FlowRuleEntity> addSingleMachineRule(FlowRuleEntity flowRuleEntity, MachineInfo machine) {
        addClusterConfig(Collections.singletonList(flowRuleEntity));
        FlowRuleEntity entity = new FlowRuleEntity();
        BeanUtils.copyProperties(flowRuleEntity, entity);
        entity.setId(null);
        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);
        entity.setLimitApp(entity.getLimitApp().trim());
        entity.setResource(entity.getResource().trim());
        entity.setApp(machine.getApp());
        entity.setIp(machine.getIp());
        entity.setPort(machine.getPort());
        ClusterFlowConfig clusterConfig = flowRuleEntity.getClusterConfig();
        try {
            entity.preInsert();
            repository.save(entity);
            List<FlowRuleEntity> rules = repository.findByParentId(flowRuleEntity.getParentId());
            ClusterFlowConfigEntity clusterFlowConfigEntity = ClusterFlowConfigEntity.fromConfig(clusterConfig);
            clusterFlowConfigEntity.setRuleId(entity.getId());
            clusterFlowConfigEntity.setFlowId(rules.get(0).getId());
            clusterFlowConfigRepository.save(clusterFlowConfigEntity);
            if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
                logger.error("Publish flow rules failed after rule add");
            }
        } catch (Throwable throwable) {
            logger.error("Failed to add flow rule", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        return Result.ofSuccess(entity);
    }

    @Override
    @Transactional
    public Result<Long> deleteSingleMachineRule(FlowRuleEntity rule) {
        try {
            repository.delete(rule.getId());
            clusterFlowConfigRepository.deleteByRuleId(rule.getId());
            if (!publishRules(rule.getApp(), rule.getIp(), rule.getPort())) {
                logger.info("publish flow rules fail after rule delete");
            }
        } catch (Exception e) {
            return Result.ofFail(-1, e.getMessage());
        }
        return Result.ofSuccess(rule.getId());
    }

    @Override
    public JpaRuleRepositoryAdapter<FlowRuleEntity> getRepository() {
        return repository;
    }

    @Override
    public boolean publishRules(String app, String ip, Integer port) {
        List<FlowRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
        addClusterConfig(rules);
        logger.info("publish rules {} to {}:{}({})", JSON.toJSONString(rules), ip, port, app);
        return sentinelApiClient.setFlowRuleOfMachine(app, ip, port, rules);
    }

    @Override
    public void publishClusterConfig() {
        List<FlowRuleEntity> clusterRules = jpaRuleStore.findAll().stream().filter(FlowRuleEntity::isClusterMode).collect(Collectors.toList());
        addClusterConfig(clusterRules);
        if (!CollectionUtils.isEmpty(clusterRules)) {
            Map<String, List<FlowRuleEntity>> appRuleMap = clusterRules.stream().collect(Collectors.groupingBy(rule -> rule.getApp()));
            for (Map.Entry<String, List<FlowRuleEntity>> stringListEntry : appRuleMap.entrySet()) {
                String app = stringListEntry.getKey();
                List<FlowRuleEntity> rules = stringListEntry.getValue();
                try {
                    List<AppClusterServerStateWrapVO> appClusterServerStateWrapVOS = clusterConfigService.getClusterUniversalState(app).thenApply(
                            ClusterEntityUtils::wrapToAppClusterServerState
                    ).get();
                    for (AppClusterServerStateWrapVO appClusterServerStateWrapVO : appClusterServerStateWrapVOS) {
                        ClusterServerStateVO state = appClusterServerStateWrapVO.getState();
                        String appName = state.getAppName();
                        if (app.equals(appName)) {
                            String id = appClusterServerStateWrapVO.getId();
                            String ip = id.split("@")[0];
                            Integer port = Integer.valueOf(id.split("@")[1]);
                            List<FlowRule> flowRules = rules.stream().map(FlowRuleEntity::toRule).distinct().collect(Collectors.toList());
                            sentinelApiClient.modifyClusterServerFlowRules(ip, port, appName, flowRules).get();
                        }
                    }
                } catch (Exception e) {
                    logger.error("modify {} cluster flow rules error", app, e);
                }
            }
        }
    }

    protected void addClusterConfig(List<FlowRuleEntity> flowRuleEntities) {
        List<Long> ruleIds = flowRuleEntities.stream().filter(FlowRuleEntity::isClusterMode).map(rule -> rule.getId()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ruleIds)) {
            List<ClusterFlowConfigEntity> flowConfigEntities = clusterFlowConfigRepository.findAllByRuleIds(ruleIds);
            Map<Long, ClusterFlowConfigEntity> clusterFlowConfigEntityMap = flowConfigEntities.stream().collect(Collectors.toMap(ClusterFlowConfigEntity::getRuleId, clusterFlowConfigEntity -> clusterFlowConfigEntity));
            flowRuleEntities.forEach(rule -> {
                ClusterFlowConfigEntity clusterFlowConfigEntity = clusterFlowConfigEntityMap.get(rule.getId());
                rule.setClusterConfig(clusterFlowConfigEntity.toConfig());
            });
        }
    }

    @Override
    protected void addExt(List<FlowRuleEntity> rules) {

    }
}
