package org.makersoft.shards.datasource;

import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Created by yanye.lj on 13-12-12.
 */
public class ShardGroupDataSource extends AbstractDriverBasedDataSource {
    private Map<Integer, DataSource> dataSourceMap;

    public void setDataSourceMap(Map<Integer, DataSource> dataSourceMap) {
        this.dataSourceMap = dataSourceMap;
    }

    @Override
    protected Connection getConnectionFromDriver(Properties props) throws SQLException {
        return null;
    }


}
