package org.makersoft.shards.rule.impl;


import org.makersoft.shards.annotation.Rule;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç4:16
 * To change this template use File | Settings | File Templates.
 */
@Rule
public class CommonTableRule extends VirtualTable {
    private String vitualTableName;

    @Override
    public String getPhysicsDbIndex(Object value) {
        Long index = (Long)value;
        //TODO regex to replce the patter;
        return String.valueOf(index);
        //return getDbNamePattern() + index % getMaxDbNum();
    }

    @Override
    public String getPhysicsTableIndex(Object value) {
        Long index = (Long)value;
        //TODO regex to replce the patter;
        return getTableNamePattern() + index % getMaxTableNum();
    }

    @Override
    public String getVitualTableName() {
        return vitualTableName;
    }

    public void setVitualTableName(String vitualTableName) {
        this.vitualTableName = vitualTableName;
    }
}
