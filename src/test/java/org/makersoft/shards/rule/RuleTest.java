package org.makersoft.shards.rule;

import org.junit.Before;
import org.junit.Test;
import org.makersoft.shards.domain.RuleUser;
import org.makersoft.shards.id.db.SequenceBolckIdGenerator;
import org.makersoft.shards.id.db.SequenceIdDao;
import org.makersoft.shards.mapper.RuleUserMapper;
import org.makersoft.shards.mapper.impl.IbatisRuleUserMapper;
import org.makersoft.shards.spring.RuleBean;
import org.makersoft.shards.utils.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.sql.Driver;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-10
 * Time: ����2:24
 * To change this template use File | Settings | File Templates.
 */

public class RuleTest {
    private ApplicationContext applicationContext;

    @Before
    public void setUp() throws Exception {
        String xmlLocation = "applicationContext-rule.xml";
        applicationContext = new ClassPathXmlApplicationContext(xmlLocation);

        Assert.notNull(applicationContext);
    }

    @Test
    public void testGetRuleBean() {
        RuleBean ruleBean = (RuleBean)applicationContext.getBean("ruleBean");
        Assert.notNull(ruleBean);

        Map<String, Rule> rules =  ruleBean.getRuleMap();
        Assert.notNull(rules);
        Assert.isTrue(rules.size() == 1);
    }

    @Test
    public void testGetMapper() {
        RuleUserMapper mapper = (RuleUserMapper)applicationContext.getBean(RuleUserMapper.class);
        Assert.notNull(mapper);
    }

    @Test
    public void testRule() {
        RuleUserMapper mapper = (RuleUserMapper)applicationContext.getBean(RuleUserMapper.class);
        RuleUser user = mapper.getById(1);

        Assert.notNull(user);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAll() {
        RuleUserMapper mapper = (RuleUserMapper)applicationContext.getBean(RuleUserMapper.class);
        List<RuleUser> userList = mapper.getAll();
    }

    @Test
    public void testGetByIdAndName() {
        RuleUserMapper mapper = (IbatisRuleUserMapper)applicationContext.getBean("ruleUserMapper");
        RuleUser user = mapper.getByIdAndName(1,"bbb");

        Assert.notNull(user);
    }

    @Test
    public void testHit() {
        RuleUserMapper mapper = (IbatisRuleUserMapper)applicationContext.getBean("ruleUserMapper");
        RuleUser user = mapper.getByHit(1, "bbb");

        Assert.notNull(user);
    }

    @Test
    public void testInsert() {
        RuleUser ruleUser = new RuleUser();
        ruleUser.setId(4);
        ruleUser.setName("def");

        RuleUserMapper mapper = (IbatisRuleUserMapper)applicationContext.getBean("ruleUserMapper");
        mapper.insert(ruleUser);
    }

    @Test
    public void testSequanceId() {
        SequenceBolckIdGenerator sequenceBolckIdGenerator = (SequenceBolckIdGenerator)applicationContext.getBean("sequanceIdGenertor");
        Long id = (Long)sequenceBolckIdGenerator.generate(null, null);
        RuleUser ruleUser = new RuleUser();
        ruleUser.setName("def");
        ruleUser.setId(id.intValue());

        RuleUserMapper mapper = (IbatisRuleUserMapper)applicationContext.getBean("ruleUserMapper");
        mapper.insert(ruleUser);
    }
}
