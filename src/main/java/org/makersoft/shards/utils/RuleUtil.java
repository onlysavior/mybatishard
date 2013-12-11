package org.makersoft.shards.utils;

import org.makersoft.shards.rule.impl.VirtualTable;
import org.springframework.util.StringUtils;

import static org.makersoft.shards.session.impl.ShardedSqlSessionImpl.*;

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
        //FIXME really need is a way to parse SQL 92
        int begin = sql.indexOf(SPLIT_BEGIN);
        int end = sql.substring(begin).indexOf(SPLIT_END) + begin + 2;
        if(begin == -1 || end == -1) {
            return sql;
        }

        String toReplce = sql.substring(begin, end);
        String actualTableName = rule.getPhysicsTableIndex(parameter);


        return sql.replace(toReplce, actualTableName);
    }

    public static String extractActualTableName(VirtualTable rule, Object parameter) {
        Object value =  extractId(parameter);
        return rule.getPhysicsTableIndex(value);
    }
}
