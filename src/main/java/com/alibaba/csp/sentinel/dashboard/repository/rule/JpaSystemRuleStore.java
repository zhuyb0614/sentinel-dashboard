package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.taodangpu.sentinel.exclude.JpaRuleStore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaSystemRuleStore extends JpaRuleStore<SystemRuleEntity> {
    @Query("select o from SystemRuleEntity o where  o.app = :app and o.ip = :ip and o.port = :port")
    List<SystemRuleEntity> findByMachine(@Param("app") String app, @Param("ip") String ip, @Param("port") Integer port);

    @Query("select o from SystemRuleEntity o where  o.app = :app")
    List<SystemRuleEntity> findByApp(@Param("app") String app);
}
