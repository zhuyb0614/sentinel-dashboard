package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.FlowRuleEntity;
import com.taodangpu.sentinel.exclude.JpaRuleStore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaFlowRuleStore extends JpaRuleStore<FlowRuleEntity> {
    @Query("select o from FlowRuleEntity o where  o.app = :app and o.ip = :ip and o.port = :port")
    List<FlowRuleEntity> findByMachine(@Param("app") String app, @Param("ip") String ip, @Param("port") Integer port);

    @Query("select o from FlowRuleEntity o where  o.app = :app")
    List<FlowRuleEntity> findByApp(@Param("app") String app);

    Long countByParentId(String parentId);
}
