package org.makersoft.shards.feature;

/**
 * Created by yanye.lj on 13-12-13.
 */
public enum FeatureEnum {
    HORIZON_SPLIT(1,"HORIZON_SPLIT"),
    READ_WRITE_SPLIT(2,"READ_WRITE_SPLIT");

    private int index;
    private String desc;

    FeatureEnum(int index, String desc) {
        this.index = index;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public int getIndex() {
        return index;
    }
}
