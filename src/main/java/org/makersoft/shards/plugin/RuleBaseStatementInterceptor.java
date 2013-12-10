package org.makersoft.shards.plugin;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.makersoft.shards.rule.Rule;
import org.makersoft.shards.rule.impl.VirtualTable;
import org.makersoft.shards.spring.RuleBean;
import org.makersoft.shards.utils.RuleUtil;

import static org.apache.ibatis.reflection.SystemMetaObject.*;
import static org.makersoft.shards.session.impl.ShardedSqlSessionImpl.*;

import java.sql.Connection;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-12-9
 * Time: ÏÂÎç9:26
 * To change this template use File | Settings | File Templates.
 */
@Intercepts({@Signature(type =StatementHandler.class, method = "prepare", args ={Connection.class})})
public class RuleBaseStatementInterceptor  implements Interceptor {
    private RuleBean ruleBean;

    public RuleBaseStatementInterceptor(RuleBean bean) {
        this.ruleBean = bean;
    }

    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaStatementHandler = MetaObject.forObject(statementHandler,
                DEFAULT_OBJECT_FACTORY, DEFAULT_OBJECT_WRAPPER_FACTORY);

        while (metaStatementHandler.hasGetter("h")) {
            Object object = metaStatementHandler.getValue("h");
            metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
                    DEFAULT_OBJECT_WRAPPER_FACTORY);
        }

        while (metaStatementHandler.hasGetter("target")) {
            Object object = metaStatementHandler.getValue("target");
            metaStatementHandler = MetaObject.forObject(object, DEFAULT_OBJECT_FACTORY,
                    DEFAULT_OBJECT_WRAPPER_FACTORY);
        }

        BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
        Object parameterObject = boundSql.getParameterObject();

        MappedStatement mappedStatement = (MappedStatement)metaStatementHandler.getValue("delegate.mappedStatement");
        String statement = mappedStatement.getId();
        String entityName = guessVitualTableName(statement, parameterObject);

        VirtualTable rule = (VirtualTable)ruleBean.getRule(entityName);

        String sql = RuleUtil.replcePlaceHolder(boundSql.getSql(), rule, parameterObject);
        metaStatementHandler.setValue("delegate.boundSql.sql", sql);
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
