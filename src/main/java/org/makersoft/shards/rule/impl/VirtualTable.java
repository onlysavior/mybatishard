package org.makersoft.shards.rule.impl;


import org.makersoft.shards.rule.Rule;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç3:18
 * To change this template use File | Settings | File Templates.
 */
public abstract class VirtualTable implements Rule {
    private String defaultDbIndex;

    private String dbNamePattern;
    private String tableNamePattern;

    private Long maxDbNum;
    private Long maxTableNum;

    private String entityName;

    private Map<String, Rule> tableRules;
    private Map<String, Rule> dbRules;

    public void init() {
        if (CollectionUtils.isEmpty(dbRules)) {
            if (StringUtils.isEmpty(defaultDbIndex)) {
                throw new IllegalArgumentException("defaultDbIndex must be set when dbRules is empty");
            }
        }

        if (CollectionUtils.isEmpty(tableRules)) {
            if (StringUtils.hasText(tableNamePattern)) {
                throw new IllegalArgumentException("tableRules must be set when tableNamePattern is not empty");
            }
        }
    }

    @Override
    public String getPhysicsDbIndex(Object value) {
        Rule dbRule = getDbRule(getVitualTableName());
        if (dbRule == null) {
            return getDefaultDbIndex();
        }

        return dbRule.getPhysicsDbIndex(value);
    }

    @Override
    public String getPhysicsTableIndex(Object value) {
        Rule tableRule = getTableRule(getVitualTableName());
        if (tableRule == null) {
            return getTableNamePattern();
        }

        return tableRule.getPhysicsDbIndex(value);
    }

    @Override
    public Rule getDbRule(String vitualTableName) {
        if (!CollectionUtils.isEmpty(dbRules)) {
            return dbRules.get(vitualTableName);
        }
        return null;
    }

    @Override
    public Rule getTableRule(String vitualTableName) {
        if (!CollectionUtils.isEmpty(tableRules)) {
            return tableRules.get(vitualTableName);
        }
        return null;
    }

    public String getDefaultDbIndex() {
        return defaultDbIndex;
    }

    public void setDefaultDbIndex(String defaultDbIndex) {
        this.defaultDbIndex = defaultDbIndex;
    }

    public String getDbNamePattern() {
        return dbNamePattern;
    }

    public void setDbNamePattern(String dbNamePattern) {
        this.dbNamePattern = dbNamePattern;
    }

    public String getTableNamePattern() {
        return tableNamePattern;
    }

    public void setTableNamePattern(String tableNamePattern) {
        this.tableNamePattern = tableNamePattern;
    }

    public Long getMaxDbNum() {
        return maxDbNum;
    }

    public void setMaxDbNum(Long maxDbNum) {
        this.maxDbNum = maxDbNum;
    }

    public Long getMaxTableNum() {
        return maxTableNum;
    }

    public void setMaxTableNum(Long maxTableNum) {
        this.maxTableNum = maxTableNum;
    }

    public Map<String, Rule> getTableRules() {
        return tableRules;
    }

    public void setTableRules(Map<String, Rule> tableRules) {
        this.tableRules = tableRules;
    }

    public Map<String, Rule> getDbRules() {
        return dbRules;
    }

    public void setDbRules(Map<String, Rule> dbRules) {
        this.dbRules = dbRules;
    }

    @Override
    public String getEntityFullName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}
