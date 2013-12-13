package org.makersoft.shards.strategy;

import org.apache.ibatis.session.RowBounds;
import org.makersoft.shards.Shard;
import org.makersoft.shards.ShardId;
import org.makersoft.shards.rule.Rule;
import org.makersoft.shards.session.impl.ShardedSqlSessionImpl;
import org.makersoft.shards.spring.RuleBean;
import org.makersoft.shards.strategy.access.ShardAccessStrategy;
import org.makersoft.shards.strategy.access.impl.ParallelShardAccessStrategy;
import org.makersoft.shards.strategy.exit.impl.RowCountExitOperation;
import org.makersoft.shards.strategy.reduce.ShardReduceStrategy;
import org.makersoft.shards.strategy.resolution.ShardResolutionStrategy;
import org.makersoft.shards.strategy.resolution.ShardResolutionStrategyData;
import org.makersoft.shards.strategy.selection.ShardSelectionStrategy;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.makersoft.shards.session.impl.ShardedSqlSessionImpl.extractId;
import static org.makersoft.shards.session.impl.ShardedSqlSessionImpl.extractVitualName;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-9
 * Time: ÏÂÎç5:37
 * To change this template use File | Settings | File Templates.
 */
public class HorizontalShardStrategyFactory implements ShardStrategyFactory {
    private RuleBean ruleBean;

    public void setRuleBean(RuleBean ruleBean) {
        this.ruleBean = ruleBean;
    }

    @Override
    public ShardStrategy newShardStrategy(List<ShardId> shardIds, List<ShardId> write, List<ShardId> read) {
        ShardSelectionStrategy pss = this.getShardSelectionStrategy(write);
        ShardResolutionStrategy prs = this.getShardResolutionStrategy(read);
        ShardAccessStrategy pas = this.getShardAccessStrategy();
        ShardReduceStrategy srs = this.getShardReduceStrategy();
        return new ShardStrategyImpl(pss, prs, pas, srs);
    }

    private ShardSelectionStrategy getShardSelectionStrategy(final List<ShardId> shardIds) {
        return new ShardSelectionStrategy() {
            //FIXME one big limit when do shard work on ORM layer
            //without rewrite the sql, it is hard to detemine the actual table name
            @Override
            public ShardId selectShardIdForNewObject(String statement, Object obj) {
                String virtualTableName = ShardedSqlSessionImpl.guessVitualTableName(statement, obj);
                Long value = Long.valueOf(String.valueOf(extractId(obj)));

                Rule rule = ruleBean.getRule(virtualTableName);
                String dbIndex = rule.getPhysicsDbIndex(value);

                for(ShardId s : shardIds) {
                    if(s.getId() == Integer.valueOf(dbIndex)) {
                        return s;
                    }
                }
                return null;
            }
        };
    }

    private ShardResolutionStrategy getShardResolutionStrategy(final List<ShardId> shardIds) {
        return new ShardResolutionStrategy() {
            @Override
            public List<ShardId> selectShardIdsFromShardResolutionStrategyData(ShardResolutionStrategyData
                                                                                       shardResolutionStrategyData) {
                Object obj = shardResolutionStrategyData.getParameter();

                if(obj == null) {
                    throw new IllegalArgumentException("must set one value to split table");
                }

                List<ShardId> rtn = new LinkedList<ShardId>();
                String entityName = shardResolutionStrategyData.getEntityName();
                Long value = (Long) extractId(obj);

                Rule rule = ruleBean.getRule(entityName);
                String dbIndex = rule.getPhysicsDbIndex(value);


                for(ShardId s : shardIds) {
                    if(s.getId() == Integer.valueOf(dbIndex)) {
                        rtn.add(s);
                    }
                }
                return rtn.size() > 0 ? rtn : null;
            }
        };
    }

    private ShardAccessStrategy getShardAccessStrategy() {
        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        };

        ThreadPoolExecutor exec = new ThreadPoolExecutor(10, 50, 60,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), factory);

        return new ParallelShardAccessStrategy(exec);
    }

    private ShardReduceStrategy getShardReduceStrategy() {
        //TODO reduce method
        return new ShardReduceStrategy() {

            @Override
            public List<Object> reduce(String statement, Object parameter, RowBounds rowBounds,
                                       List<Object> values) {
                if(statement.endsWith("getAllCount")){

                    return new RowCountExitOperation().apply(values);
                }

                return values;
            }
        };
    }
}
