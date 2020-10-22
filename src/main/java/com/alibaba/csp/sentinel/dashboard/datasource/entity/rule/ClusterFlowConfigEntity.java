package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.block.ClusterRuleConstant;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;

/**
 * @author zhuyb
 * @date 10/12/2020 3:13 PM
 * 集群配置
 */
@Table
@Entity
public class ClusterFlowConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long ruleId;
    /**
     * Global unique ID.
     */
    private Long flowId;

    /**
     * Threshold type (average by local value or global value).
     */
    private int thresholdType = ClusterRuleConstant.FLOW_THRESHOLD_AVG_LOCAL;
    private boolean fallbackToLocalWhenFail = true;

    /**
     * 0: normal.
     */
    private int strategy = ClusterRuleConstant.FLOW_CLUSTER_STRATEGY_NORMAL;

    private int sampleCount = ClusterRuleConstant.DEFAULT_CLUSTER_SAMPLE_COUNT;
    /**
     * The time interval length of the statistic sliding window (in milliseconds)
     */
    private int windowIntervalMs = RuleConstant.DEFAULT_WINDOW_INTERVAL_MS;


    public Long getFlowId() {
        return flowId;
    }


    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }


    public int getThresholdType() {
        return thresholdType;
    }


    public void setThresholdType(int thresholdType) {
        this.thresholdType = thresholdType;
    }


    public boolean isFallbackToLocalWhenFail() {
        return fallbackToLocalWhenFail;
    }


    public void setFallbackToLocalWhenFail(boolean fallbackToLocalWhenFail) {
        this.fallbackToLocalWhenFail = fallbackToLocalWhenFail;
    }


    public int getStrategy() {
        return strategy;
    }


    public void setStrategy(int strategy) {
        this.strategy = strategy;
    }


    public int getSampleCount() {
        return sampleCount;
    }


    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }


    public int getWindowIntervalMs() {
        return windowIntervalMs;
    }


    public void setWindowIntervalMs(int windowIntervalMs) {
        this.windowIntervalMs = windowIntervalMs;
    }

    public static ClusterFlowConfigEntity fromConfig(ClusterFlowConfig config) {
        ClusterFlowConfigEntity entity = new ClusterFlowConfigEntity();
        BeanUtils.copyProperties(config, entity);
        return entity;
    }

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

    public ClusterFlowConfig toConfig() {
        ClusterFlowConfig clusterFlowConfig = new ClusterFlowConfig();
        BeanUtils.copyProperties(this, clusterFlowConfig);
        return clusterFlowConfig;
    }
}
