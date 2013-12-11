package org.makersoft.shards.rule.impl;


import org.makersoft.shards.annotation.Rule;

import java.util.Map;

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
        Object targetValue = null;
        if(value instanceof String || value instanceof Number) {
            targetValue = value;
        } else if(value instanceof Map) {
            Map map = (Map)value;
            targetValue = map.get("id");
        }
        Long index = Long.valueOf(String.valueOf(targetValue));
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
