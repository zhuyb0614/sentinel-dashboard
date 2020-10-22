package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.block.ClusterRuleConstant;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;

/**
 * @author zhuyb
 * @date 10/15/2020 1:36 PM
 * 热点参数流控集群配置
 */
@Table
@Entity
public class ClusterParamFlowConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ruleId;
    /**
     * Global unique ID.
     */
    private Long flowId;

    /**
     * Threshold type (average by local value or global value).
     */
    private int thresholdType = ClusterRuleConstant.FLOW_THRESHOLD_AVG_LOCAL;
    private boolean fallbackToLocalWhenFail = false;

    private int sampleCount = ClusterRuleConstant.DEFAULT_CLUSTER_SAMPLE_COUNT;
    /**
     * The time interval length of the statistic sliding window (in milliseconds)
     */
    private int windowIntervalMs = RuleConstant.DEFAULT_WINDOW_INTERVAL_MS;

    public static ClusterParamFlowConfigEntity fromConfig(ParamFlowClusterConfig clusterConfig) {
        ClusterParamFlowConfigEntity clusterParamFlowConfigEntity = new ClusterParamFlowConfigEntity();
        BeanUtils.copyProperties(clusterConfig, clusterParamFlowConfigEntity);
        return clusterParamFlowConfigEntity;
    }

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

    public ParamFlowClusterConfig toConfig() {
        ParamFlowClusterConfig paramFlowClusterConfig = new ParamFlowClusterConfig();
        BeanUtils.copyProperties(this, paramFlowClusterConfig);
        return paramFlowClusterConfig;
    }
}