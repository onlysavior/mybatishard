package org.makersoft.shards.mapper.impl;

import org.makersoft.shards.domain.RuleUser;
import org.makersoft.shards.mapper.RuleUserMapper;
import org.mybatis.spring.SqlSessionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yanye.lj on 13-12-11.
 */

public class IbatisRuleUserMapper implements RuleUserMapper {
    private SqlSessionTemplate sqlSessionTemplate;

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @Override
    public RuleUser getById(long id) {
        return null;
    }

    @Override
    public List<RuleUser> getAll() {
        return null;
    }

    @Override
    public RuleUser getByIdAndName(long id, String name) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("id", id);
        param.put("name", name);

        return sqlSessionTemplate.selectOne("org.makersoft.shards.mapper.RuleUserMapper.getByIdAndName", param);
    }

    @Override
    public RuleUser getByHit(long id, String name) {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("id", id);
        param.put("name", name);

        return sqlSessionTemplate.selectOne("org.makersoft.shards.mapper.RuleUserMapper.getByHit", param);
    }

    @Override
    public void insert(RuleUser user) {
       sqlSessionTemplate.insert("org.makersoft.shards.mapper.RuleUserMapper.insert", user);
    }
}
