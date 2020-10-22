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
package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowRuleEntity;
import com.taodangpu.sentinel.exclude.JpaRuleStore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface JpaParamFlowRuleStore extends JpaRuleStore<ParamFlowRuleEntity> {
    @Query("select pfr from  ParamFlowRuleEntity pfr where  pfr.app=:app and pfr.ip=:ip and pfr.port = :port")
    @Override
    List<ParamFlowRuleEntity> findByMachine(@Param("app") String app, @Param("ip") String ip, @Param("port") Integer port);
}
