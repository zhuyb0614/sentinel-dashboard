package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.ParamFlowItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author zhuyb
 * @date 10/22/2020 12:01 PM
 * 附件参数
 */
public interface ParamFlowItemRepository extends JpaRepository<ParamFlowItemEntity, Long> {
    int deleteByRuleId(Long ruleId);
    @Query("select pfi from ParamFlowItemEntity pfi  where  pfi.ruleId in (:ruleIds)")
    List<ParamFlowItemEntity> findAllByRuleIds(@Param("ruleIds") List<Long> ruleIds);
}
