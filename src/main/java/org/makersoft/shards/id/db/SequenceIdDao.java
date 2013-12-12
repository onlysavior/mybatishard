package org.makersoft.shards.id.db;

import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by yanye.lj on 13-12-12.
 */
public class SequenceIdDao {
    private static final Logger logger = Logger.getLogger(SequenceIdDao.class);
    private static final int DEFAULT_MAX_TRY = 3;
    private static final long DEFAULT_STEP = 1000;

    private DataSource dataSource;
    private String tableName;
    private String valueColumnName;
    private String nameColumnName;

    private Integer maxTryTime = DEFAULT_MAX_TRY;
    private Long step = DEFAULT_STEP;

    public IdBlock nextIdBlock(String name) throws SQLException {

        Connection connection;
        PreparedStatement statement;
        ResultSet rs;

        long oldValue;
        long newValue;
        for (int index = 0; index < maxTryTime; index++) {
            connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            statement = connection.prepareStatement(getSelectSql());
            statement.setString(1, name);

            rs = statement.executeQuery();
            rs.next();

            oldValue = rs.getLong(1);

            if (oldValue < 0) {
                throw new IllegalStateException("Sequance value invild, column name is " + name);
            }

            newValue = oldValue + DEFAULT_STEP;

            if (newValue > Long.MAX_VALUE - 10000L) {
                throw new IllegalStateException("Sequance value overflow, column name is " + name);
            }

            rs.close();
            rs = null;
            statement.close();
            statement = null;
            connection.close();
            connection = null;

            connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            statement = connection.prepareStatement(getUpdateSql());
            statement.setLong(1, newValue);
            statement.setString(2, name);

            if (statement.executeUpdate() <= 0) {
                continue;
            }

            statement.close();
            statement = null;
            connection.close();
            connection = null;

            return new IdBlock(oldValue + 1, newValue);
        }

        throw new AssertionError("unreachable code, sequanceId retry greater then " + maxTryTime);
    }

    private String getSelectSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(getValueColumnName())
                .append(" FROM ").append(getTableName())
                .append(" WHERE ").append(getNameColumnName())
                .append(" = ").append("?");

        return sb.toString();
    }

    private String getUpdateSql() {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(getTableName())
                .append(" SET ").append(getValueColumnName())
                .append(" = ").append("?")
                .append(" WHERE ").append(getNameColumnName())
                .append(" = ").append("?");

        return sb.toString();
    }

    public void setMaxTryTime(Integer maxTryTime) {
        this.maxTryTime = maxTryTime;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setValueColumnName(String valueColumnName) {
        this.valueColumnName = valueColumnName;
    }

    public void setNameColumnName(String nameColumnName) {
        this.nameColumnName = nameColumnName;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getTableName() {
        return tableName;
    }

    public String getValueColumnName() {
        return valueColumnName;
    }

    public String getNameColumnName() {
        return nameColumnName;
    }
}
