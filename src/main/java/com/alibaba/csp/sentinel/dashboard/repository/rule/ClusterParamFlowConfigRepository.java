package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ClusterParamFlowConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author zhuyb
 * @date 10/15/2020 1:49 PM
 * 热点参数流控
 */
public interface ClusterParamFlowConfigRepository extends JpaRepository<ClusterParamFlowConfigEntity, Long> {
    @Query("select cfc from ClusterParamFlowConfigEntity  cfc where  cfc.ruleId in (:ruleIds)")
    List<ClusterParamFlowConfigEntity> findAllByRuleIds(@Param("ruleIds") List<Long> ruleIds);

    int deleteByRuleId(Long ruleId);
}
