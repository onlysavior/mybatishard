package org.makersoft.shards.spring;

import org.makersoft.shards.rule.Rule;
import org.makersoft.shards.rule.impl.DirectTableRule;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç5:19
 * To change this template use File | Settings | File Templates.
 */
public class RuleBean implements BeanPostProcessor{
    private Map<String, Rule> ruleMap = new ConcurrentHashMap<String, Rule>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        org.makersoft.shards.annotation.Rule annotation =
                (org.makersoft.shards.annotation.Rule)clazz.getAnnotation(org.makersoft.shards.annotation.Rule.class);
        if(annotation != null) {
            Rule target = (Rule)bean;
            ruleMap.put(target.getEntityFullName(), target);
        }

        return bean;
    }

    public Map<String, Rule> getRuleMap() {
        return ruleMap;
    }

    public Rule getRule(String entityName) {
        Rule rule = ruleMap.get(entityName);
        if(rule != null) {
            return rule;
        }
        return DirectTableRule.INSTANCE;
    }
}
