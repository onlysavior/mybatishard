package org.makersoft.shards.mapper;

import org.makersoft.shards.annotation.MyBatisMapper;
import org.makersoft.shards.domain.RuleUser;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-10
 * Time: обнГ1:55
 * To change this template use File | Settings | File Templates.
 */
@MyBatisMapper(entityName = "org.makersoft.shards.domain.RuleUser")
public interface RuleUserMapper {
    RuleUser getById(long id);

    List<RuleUser> getAll();
}
