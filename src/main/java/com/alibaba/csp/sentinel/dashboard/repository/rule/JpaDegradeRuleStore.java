package com.alibaba.csp.sentinel.dashboard.repository.rule;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.taodangpu.sentinel.exclude.JpaRuleStore;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaDegradeRuleStore extends JpaRuleStore<DegradeRuleEntity> {

    @Query("select o from DegradeRuleEntity o where  o.app = :app and o.ip = :ip and o.port = :port")
    List<DegradeRuleEntity> findByMachine(@Param("app") String app, @Param("ip") String ip, @Param("port") Integer port);

    @Query("select o from DegradeRuleEntity o where  o.app = :app")
    List<DegradeRuleEntity> findByApp(@Param("app") String app);

    @Query("delete from DegradeRuleEntity o where  o.parentId = :parentId")
    @Modifying
    int deleteByParentId(@Param("parentId") String parentId);
}
