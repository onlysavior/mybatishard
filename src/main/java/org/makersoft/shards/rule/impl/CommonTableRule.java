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
        if(value == null) {
            return getDefaultDbIndex(); //TODO maybe no need
        }
        Long index = (Long)value;
        return String.valueOf(index);
    }

    @Override
    public String getPhysicsTableIndex(Object value) {
        if(value == null) {

        }
        Long index = (Long)value;
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
