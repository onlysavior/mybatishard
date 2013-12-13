package org.makersoft.shards.datasource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * Created by yanye.lj on 13-12-13.
 */
public class ShardAtomDataSource extends SimpleDriverDataSource {
    public static final String DEFAULT_TYPE = "R";
    public static final String READ_TYPE = "R";
    public static final String WRITE_TYPE = "W";
    private String type = DEFAULT_TYPE;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
