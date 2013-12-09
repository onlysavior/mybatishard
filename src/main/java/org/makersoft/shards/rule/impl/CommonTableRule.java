package org.makersoft.shards.rule.impl;



/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç4:16
 * To change this template use File | Settings | File Templates.
 */
public class CommonTableRule extends VirtualTable {
    private String vitualTableName;

    @Override
    public String getPhysicsDbIndex(Object value) {
        Long index = Long.valueOf((String)value);
        //TODO regex to replce the patter;
        return getDbNamePattern() + index % getMaxDbNum();
    }

    @Override
    public String getPhysicsTableIndex(Object value) {
        Long index = Long.valueOf((String)value);
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
