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
package com.alibaba.csp.sentinel.dashboard.controller;

import com.alibaba.csp.sentinel.dashboard.auth.AuthService;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.AuthUser;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.service.DegradeService;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * @author leyou
 */
@Controller
@RequestMapping(value = "/degrade", produces = MediaType.APPLICATION_JSON_VALUE)
public class DegradeController {

    private final Logger logger = LoggerFactory.getLogger(DegradeController.class);

    @Autowired
    private DegradeService degradeService;

    @Autowired
    private AuthService<HttpServletRequest> authService;

    @ResponseBody
    @RequestMapping("/rules.json")
    public Result<List<DegradeRuleEntity>> queryMachineRules(HttpServletRequest request, String app, String ip, Integer port) {
        AuthUser authUser = authService.getAuthUser(request);
        authUser.authTarget(app, PrivilegeType.READ_RULE);

        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }
        try {
            return Result.ofSuccess(degradeService.machineRules(MachineInfo.of(app, ip, port)));
        } catch (Throwable throwable) {
            logger.error("queryApps error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }

    @ResponseBody
    @RequestMapping("/new.json")
    public Result<DegradeRuleEntity> add(HttpServletRequest request,
                                         String app, String ip, Integer port, String limitApp, String resource,
                                         Double count, Integer timeWindow, Integer grade) {
        AuthUser authUser = authService.getAuthUser(request);
        authUser.authTarget(app, PrivilegeType.WRITE_RULE);

        if (StringUtil.isBlank(app)) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isBlank(ip)) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (port == null) {
            return Result.ofFail(-1, "port can't be null");
        }
        if (StringUtil.isBlank(limitApp)) {
            return Result.ofFail(-1, "limitApp can't be null or empty");
        }
        if (StringUtil.isBlank(resource)) {
            return Result.ofFail(-1, "resource can't be null or empty");
        }
        if (count == null) {
            return Result.ofFail(-1, "count can't be null");
        }
        if (timeWindow == null) {
            return Result.ofFail(-1, "timeWindow can't be null");
        }
        if (grade == null) {
            return Result.ofFail(-1, "grade can't be null");
        }
        if (grade < RuleConstant.DEGRADE_GRADE_RT || grade > RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT) {
            return Result.ofFail(-1, "Invalid grade: " + grade);
        }
        DegradeRuleEntity entity = new DegradeRuleEntity();
        entity.setApp(app.trim());
        entity.setIp(ip.trim());
        entity.setPort(port);
        entity.setLimitApp(limitApp.trim());
        entity.setResource(resource.trim());
        entity.setCount(count);
        entity.setTimeWindow(timeWindow);
        entity.setGrade(grade);
        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);
        return degradeService.addAllMachineRule(entity);
    }

    @ResponseBody
    @RequestMapping("/save.json")
    public Result<DegradeRuleEntity> updateIfNotNull(HttpServletRequest request,
                                                     Long id, String app, String limitApp, String resource,
                                                     Double count, Integer timeWindow, Integer grade) {
        AuthUser authUser = authService.getAuthUser(request);
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }
        if (grade != null) {
            if (grade < RuleConstant.DEGRADE_GRADE_RT || grade > RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT) {
                return Result.ofFail(-1, "Invalid grade: " + grade);
            }
        }
        DegradeRuleEntity entity = degradeService.getRepository().findById(id);
        if (entity == null) {
            return Result.ofFail(-1, "id " + id + " dose not exist");
        }
        authUser.authTarget(entity.getApp(), PrivilegeType.WRITE_RULE);
        if (StringUtil.isNotBlank(app)) {
            entity.setApp(app.trim());
        }

        if (StringUtil.isNotBlank(limitApp)) {
            entity.setLimitApp(limitApp.trim());
        }
        if (StringUtil.isNotBlank(resource)) {
            entity.setResource(resource.trim());
        }
        if (count != null) {
            entity.setCount(count);
        }
        if (timeWindow != null) {
            entity.setTimeWindow(timeWindow);
        }
        if (grade != null) {
            entity.setGrade(grade);
        }
        Date date = new Date();
        entity.setGmtModified(date);
        try {
            entity = degradeService.getRepository().save(entity);
        } catch (Throwable throwable) {
            logger.error("save error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        if (!degradeService.publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
            logger.info("publish degrade rules fail after rule update");
        }
        return Result.ofSuccess(entity);
    }

    @ResponseBody
    @RequestMapping("/delete.json")
    public Result<Long> delete(HttpServletRequest request, Long id) {
        AuthUser authUser = authService.getAuthUser(request);
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }

        DegradeRuleEntity oldEntity = degradeService.getRepository().findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        authUser.authTarget(oldEntity.getApp(), PrivilegeType.DELETE_RULE);
        List<DegradeRuleEntity> rules = degradeService.getRepository().findByParentId(oldEntity.getParentId());
        return degradeService.deleteAllMachineRule(rules);
    }

}
