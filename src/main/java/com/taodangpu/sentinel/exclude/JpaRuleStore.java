package com.taodangpu.sentinel.exclude;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaRuleStore<T extends RuleEntity> extends JpaRepository<T, Long> {

    List<T> findByMachine(@Param("app") String app, @Param("ip") String ip, @Param("port") Integer port);

    List<T> findByApp(@Param("appName") String appName);

    List<T> findByParentId(@Param("parentId") String parentId);
}
