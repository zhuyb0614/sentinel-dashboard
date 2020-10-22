package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ClusterParamFlowConfigEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowItemEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.AppClusterServerStateWrapVO;
import com.alibaba.csp.sentinel.dashboard.domain.cluster.state.ClusterServerStateVO;
import com.alibaba.csp.sentinel.dashboard.repository.rule.ClusterParamFlowConfigRepository;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaParamFlowRuleStore;
import com.alibaba.csp.sentinel.dashboard.repository.rule.JpaRuleRepositoryAdapter;
import com.alibaba.csp.sentinel.dashboard.repository.rule.ParamFlowItemRepository;
import com.alibaba.csp.sentinel.dashboard.util.ClusterEntityUtils;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhuyb
 * @date 10/12/2020 2:29 PM
 * 热点参数批量处理
 */
@Service
public class ParamFlowRuleService extends ClusterBatchRuleService<ParamFlowRuleEntity> {
    private final Logger logger = LoggerFactory.getLogger(ParamFlowRuleService.class);
    @Autowired
    private SentinelApiClient sentinelApiClient;
    @Autowired
    private JpaRuleRepositoryAdapter<ParamFlowRuleEntity> repository;
    @Autowired
    private JpaParamFlowRuleStore jpaRuleStore;
    @Autowired
    private ClusterParamFlowConfigRepository clusterParamFlowConfigRepository;
    @Autowired
    private ParamFlowItemRepository paramFlowItemRepository;

    @PostConstruct
    public void init() {
        repository.setJpaRuleStore(jpaRuleStore);
    }

    @Override
    protected Result<ParamFlowRuleEntity> addSingleMachineRule(ParamFlowRuleEntity paramFlowRuleEntity, MachineInfo machine) {
        ParamFlowRuleEntity entity = new ParamFlowRuleEntity();
        BeanUtils.copyProperties(paramFlowRuleEntity, entity);
        entity.setId(null);
        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);
        entity.setLimitApp(entity.getLimitApp().trim());
        entity.setResource(entity.getResource().trim());
        entity.setApp(machine.getApp());
        entity.setIp(machine.getIp());
        entity.setPort(machine.getPort());
        try {
            repository.save(entity);
            List<ParamFlowRuleEntity> rules = repository.findByParentId(paramFlowRuleEntity.getParentId());
            ParamFlowClusterConfig clusterConfig = paramFlowRuleEntity.getClusterConfig();
            ClusterParamFlowConfigEntity clusterFlowConfigEntity = ClusterParamFlowConfigEntity.fromConfig(clusterConfig);
            clusterFlowConfigEntity.setRuleId(entity.getId());
            clusterFlowConfigEntity.setFlowId(rules.get(0).getId());
            clusterParamFlowConfigRepository.save(clusterFlowConfigEntity);
            List<ParamFlowItem> paramFlowItemList = paramFlowRuleEntity.getParamFlowItemList();
            if (!CollectionUtils.isEmpty(paramFlowItemList)) {
                List<ParamFlowItemEntity> itemEntities = paramFlowItemList.stream().map(ParamFlowItemEntity::fromItem).collect(Collectors.toList());
                itemEntities.forEach(paramFlowItemEntity -> {
                    paramFlowItemEntity.setRuleId(entity.getId());
                });
                paramFlowItemRepository.saveAll(itemEntities);
            }
            if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
                logger.error("Publish param flow rules failed after rule add");
            }
        } catch (Throwable throwable) {
            logger.error("Failed to add param flow rule", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        return Result.ofSuccess(entity);
    }

    @Override
    protected Result<Long> deleteSingleMachineRule(ParamFlowRuleEntity rule) {
        try {
            repository.delete(rule.getId());
            clusterParamFlowConfigRepository.deleteByRuleId(rule.getId());
            paramFlowItemRepository.deleteByRuleId(rule.getId());
            if (!publishRules(rule.getApp(), rule.getIp(), rule.getPort())) {
                logger.info("publish flow rules fail after rule delete");
            }
        } catch (Exception e) {
            return Result.ofFail(-1, e.getMessage());
        }
        return Result.ofSuccess(rule.getId());
    }

    @Override
    public JpaRuleRepositoryAdapter<ParamFlowRuleEntity> getRepository() {
        return repository;
    }

    @Override
    public boolean publishRules(String app, String ip, Integer port) {
        List<ParamFlowRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
        addClusterConfig(rules);
        addParamItems(rules);
        try {
            sentinelApiClient.setParamFlowRuleOfMachine(app, ip, port, rules).get();
            return true;
        } catch (Exception e) {
            logger.error("publish flow rules error ", e);
        }
        return false;
    }

    @Override
    public void publishClusterConfig() {
        List<ParamFlowRuleEntity> clusterRules = jpaRuleStore.findAll().stream().filter(ParamFlowRuleEntity::isClusterMode).collect(Collectors.toList());
        addClusterConfig(clusterRules);
        if (!CollectionUtils.isEmpty(clusterRules)) {
            Map<String, List<ParamFlowRuleEntity>> appRuleMap = clusterRules.stream().collect(Collectors.groupingBy(rule -> rule.getApp()));
            for (Map.Entry<String, List<ParamFlowRuleEntity>> stringListEntry : appRuleMap.entrySet()) {
                String app = stringListEntry.getKey();
                List<ParamFlowRuleEntity> rules = stringListEntry.getValue();
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
                            List<ParamFlowRule> flowRules = rules.stream().map(ParamFlowRuleEntity::toRule).distinct().collect(Collectors.toList());
                            sentinelApiClient.modifyClusterServerParamFlowRules(ip, port, appName, flowRules).get();
                        }
                    }
                } catch (Exception e) {
                    logger.error("modify {} cluster flow rules error", app, e);
                }
            }
        }
    }

    protected void addClusterConfig(List<ParamFlowRuleEntity> paramFlowRuleEntities) {
        List<Long> ruleIds = paramFlowRuleEntities.stream().filter(ParamFlowRuleEntity::isClusterMode).map(rule -> rule.getId()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ruleIds)) {
            List<ClusterParamFlowConfigEntity> flowConfigEntities = clusterParamFlowConfigRepository.findAllByRuleIds(ruleIds);
            Map<Long, ClusterParamFlowConfigEntity> clusterFlowConfigEntityMap = flowConfigEntities.stream().collect(Collectors.toMap(ClusterParamFlowConfigEntity::getRuleId, clusterFlowConfigEntity -> clusterFlowConfigEntity));
            paramFlowRuleEntities.forEach(rule -> {
                ClusterParamFlowConfigEntity clusterParamFlowConfigEntity = clusterFlowConfigEntityMap.get(rule.getId());
                if (clusterParamFlowConfigEntity != null) {
                    rule.setClusterConfig(clusterParamFlowConfigEntity.toConfig());
                }
            });
        }
    }

    @Override
    protected void addExt(List<ParamFlowRuleEntity> rules) {
        addParamItems(rules);
    }

    private void addParamItems(List<ParamFlowRuleEntity> ruleEntities) {
        List<Long> ruleIds = ruleEntities.stream().filter(ParamFlowRuleEntity::isClusterMode).map(rule -> rule.getId()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ruleIds)) {
            List<ParamFlowItemEntity> paramFlowItemEntities = paramFlowItemRepository.findAllByRuleIds(ruleIds);
            Map<Long, List<ParamFlowItemEntity>> ruleItems = paramFlowItemEntities.stream().collect(Collectors.groupingBy(paramFlowItemEntity -> paramFlowItemEntity.getRuleId()));
            ruleEntities.forEach(rule -> {
                List<ParamFlowItemEntity> itemEntities = ruleItems.get(rule.getId());
                if (itemEntities != null) {
                    rule.setParamFlowItemList(itemEntities.stream().map(ParamFlowItemEntity::toItem).collect(Collectors.toList()));
                }
            });
        }
    }

}
