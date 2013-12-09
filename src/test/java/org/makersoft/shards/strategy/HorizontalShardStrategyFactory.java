package org.makersoft.shards.strategy;

import org.makersoft.shards.ShardId;
import org.makersoft.shards.rule.Rule;
import org.makersoft.shards.spring.RuleBean;
import org.makersoft.shards.strategy.access.ShardAccessStrategy;
import org.makersoft.shards.strategy.reduce.ShardReduceStrategy;
import org.makersoft.shards.strategy.resolution.ShardResolutionStrategy;
import org.makersoft.shards.strategy.selection.ShardSelectionStrategy;

import java.util.List;
import java.util.Map;

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
    public ShardStrategy newShardStrategy(List<ShardId> shardIds) {
        ShardSelectionStrategy pss = this.getShardSelectionStrategy(shardIds);
        ShardResolutionStrategy prs = this.getShardResolutionStrategy(shardIds);
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
                String virtualTableName = extractVitualName(obj);
                Long value = (Long) extractId(obj);

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


}
