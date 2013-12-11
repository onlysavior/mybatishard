package org.makersoft.shards.utils;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSourceImpl;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitorUtils;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.alibaba.druid.util.JdbcUtils;
import org.makersoft.shards.rule.impl.VirtualTable;

import java.util.List;

import static org.makersoft.shards.session.impl.ShardedSqlSessionImpl.extractId;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-10
 * Time: ÉÏÎç10:20
 * To change this template use File | Settings | File Templates.
 */
public class RuleUtil {
    public static final String SPLIT_BEGIN = "@{";
    public static final String SPLIT_END = "}@";

    public static String replcePlaceHolder(String sql,
                                    VirtualTable rule,
                                    Object parameter) {
        String actualTableName = rule.getPhysicsTableIndex(parameter);
        RuleSQLVisitor ruleSQLVisitor = new RuleSQLVisitor(actualTableName);

        List<SQLStatement> statements = SQLUtils.parseStatements(sql, JdbcUtils.MYSQL);
        StringBuilder sb = new StringBuilder();
        MySqlOutputVisitor mySqlOutputVisitor = new MySqlOutputVisitor(sb);
        for(SQLStatement s : statements) {
            s.accept(ruleSQLVisitor);
            s.accept(mySqlOutputVisitor);
        }

        return sb.toString();

    }

    private static class RuleSQLVisitor extends MySqlASTVisitorAdapter {
        private final String actualName;

        public RuleSQLVisitor(String actualName) {
            this.actualName = actualName;
        }

        @Override
        public boolean visit(MySqlSelectQueryBlock x) {
            x.setFrom(new SQLExprTableSource(new SQLIdentifierExpr(actualName)));
            return super.visit(x);
        }

        @Override
        public boolean visit(MySqlDeleteStatement x) {
           x.getFrom().setAlias(actualName);
           return super.visit(x);
        }

        @Override
        public boolean visit(MySqlUpdateStatement x) {
            x.setTableSource(new SQLExprTableSource(new SQLIdentifierExpr(actualName)));
            return super.visit(x);
        }

        @Override
        public boolean visit(MySqlInsertStatement x) {
            x.setTableName(new SQLIdentifierExpr(actualName));
           return super.visit(x);
        }
    }

}
