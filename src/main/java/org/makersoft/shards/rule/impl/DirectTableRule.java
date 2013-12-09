package org.makersoft.shards.rule.impl;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç5:53
 * To change this template use File | Settings | File Templates.
 */
public class DirectTableRule extends VirtualTable{
    public static final DirectTableRule INSTANCE = new DirectTableRule();

    private DirectTableRule() {}
    private String vitualTableName;

    @Override
    public String getVitualTableName() {
        return vitualTableName;
    }

    public void setVitualTableName(String vitualTableName) {
        this.vitualTableName = vitualTableName;
    }

    @Override
    public String getPhysicsDbIndex(Object value) {
        return getDefaultDbIndex();
    }

    @Override
    public String getPhysicsTableIndex(Object value) {
        return vitualTableName;
    }
}
