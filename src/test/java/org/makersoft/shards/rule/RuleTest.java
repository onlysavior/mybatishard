package org.makersoft.shards.rule;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.makersoft.shards.domain.RuleUser;
import org.makersoft.shards.mapper.RuleUserMapper;
import org.makersoft.shards.spring.RuleBean;
import org.makersoft.shards.utils.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-10
 * Time: ÏÂÎç2:24
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



}
