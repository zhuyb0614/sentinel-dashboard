package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ClusterFlowConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author zhuyb
 * @date 10/12/2020 3:17 PM
 * 集群配置
 */
public interface ClusterFlowConfigRepository extends JpaRepository<ClusterFlowConfigEntity, Long> {
    @Query("select cfc from ClusterFlowConfigEntity  cfc where  cfc.ruleId in (:ruleIds)")
    List<ClusterFlowConfigEntity> findAllByRuleIds(@Param("ruleIds") List<Long> ruleIds);

    int deleteByRuleId(Long ruleId);
}
