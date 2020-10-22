/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.datasource.entity.rule;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowClusterConfig;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.util.*;

/**
 * @author Eric Zhao
 * @since 0.2.1
 */
@Table
@Entity
public class ParamFlowRuleEntity implements RuleEntity, BatchRuleEntity {
    protected Long id;
    private String parentId;
    protected String app;
    protected String ip;
    protected Integer port;

    private Date gmtCreate;
    private Date gmtModified;
    /**
     * The threshold type of flow control (0: thread count, 1: QPS).
     */
    private int grade = RuleConstant.FLOW_GRADE_QPS;

    /**
     * Parameter index.
     */
    private Integer paramIdx;

    /**
     * The threshold count.
     */
    private double count;

    /**
     * Traffic shaping behavior (since 1.6.0).
     */
    private int controlBehavior = RuleConstant.CONTROL_BEHAVIOR_DEFAULT;

    private int maxQueueingTimeMs = 0;
    private int burstCount = 0;
    private long durationInSec = 1;
    /**
     * Resource name.
     */
    private String resource;

    /**
     * <p>
     * Application name that will be limited by origin.
     * The default limitApp is {@code default}, which means allowing all origin apps.
     * </p>
     * <p>
     * For authority rules, multiple origin name can be separated with comma (',').
     * </p>
     */
    private String limitApp;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getLimitApp() {
        return limitApp;
    }

    public void setLimitApp(String limitApp) {
        this.limitApp = limitApp;
    }

    /**
     * Original exclusion items of parameters.
     */
    @Transient
    private List<ParamFlowItem> paramFlowItemList = new ArrayList<ParamFlowItem>();

    /**
     * Parsed exclusion items of parameters. Only for internal use.
     */
    private Map<Object, Integer> hotItems = new HashMap<Object, Integer>();

    /**
     * Indicating whether the rule is for cluster mode.
     */
    private boolean clusterMode = false;
    /**
     * Cluster mode specific config for parameter flow rule.
     */
    private ParamFlowClusterConfig clusterConfig;


    public ParamFlowRuleEntity() {
    }

    public static ParamFlowRuleEntity fromAuthorityRule(String app, String ip, Integer port, ParamFlowRule rule) {
        ParamFlowRuleEntity entity = new ParamFlowRuleEntity();
        BeanUtils.copyProperties(rule, entity);
        entity.setApp(app);
        entity.setIp(ip);
        entity.setPort(port);
        return entity;
    }

    @Override
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    private ParamFlowRule rule;

    @Transient
    public ParamFlowRule getRule() {
        return toRule();
    }

    public void setRule(ParamFlowRule rule) {
        this.rule = rule;
        BeanUtils.copyProperties(rule, this);
        this.setParamFlowItemList(rule.getParamFlowItemList());
    }

    @Override
    public ParamFlowRule toRule() {
        if (rule == null) {
            rule = new ParamFlowRule();
            BeanUtils.copyProperties(this, rule);
        }
        return rule;
    }

    @Override
    public void preInsert() {
        if (isClusterMode()) {
            ParamFlowClusterConfig config = getClusterConfig();
            if (config == null) {
                config = new ParamFlowClusterConfig();
                // Set cluster rule id.
                config.setFlowId(getId());
            }
        }
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    @Override
    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    @Override
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public int getGrade() {
        return grade;
    }

    public void setGrade(int grade) {
        this.grade = grade;
    }

    public Integer getParamIdx() {
        return paramIdx;
    }

    public void setParamIdx(Integer paramIdx) {
        this.paramIdx = paramIdx;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public int getControlBehavior() {
        return controlBehavior;
    }

    public void setControlBehavior(int controlBehavior) {
        this.controlBehavior = controlBehavior;
    }

    public int getMaxQueueingTimeMs() {
        return maxQueueingTimeMs;
    }

    public void setMaxQueueingTimeMs(int maxQueueingTimeMs) {
        this.maxQueueingTimeMs = maxQueueingTimeMs;
    }

    public int getBurstCount() {
        return burstCount;
    }

    public void setBurstCount(int burstCount) {
        this.burstCount = burstCount;
    }

    public long getDurationInSec() {
        return durationInSec;
    }

    public void setDurationInSec(long durationInSec) {
        this.durationInSec = durationInSec;
    }

    @Transient
    public List<ParamFlowItem> getParamFlowItemList() {
        return paramFlowItemList;
    }

    public void setParamFlowItemList(List<ParamFlowItem> paramFlowItemList) {
        this.paramFlowItemList = paramFlowItemList;
    }

    @Transient
    public Map<Object, Integer> getHotItems() {
        return hotItems;
    }

    public void setHotItems(Map<Object, Integer> hotItems) {
        this.hotItems = hotItems;
    }

    public boolean isClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(boolean clusterMode) {
        this.clusterMode = clusterMode;
    }

    @Transient
    public ParamFlowClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ParamFlowClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

}
