package org.makersoft.shards.rule;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: обнГ3:34
 * To change this template use File | Settings | File Templates.
 */
public interface Rule {
    Map<String, Rule> getDbRules();
    Map<String, Rule> getTableRules();
    Rule getDbRule(String vitualTableName);
    Rule getTableRule(String vitualTableName);


    String getPhysicsDbIndex(Object value);
    String getPhysicsTableIndex(Object value);
    String getVitualTableName();

}
