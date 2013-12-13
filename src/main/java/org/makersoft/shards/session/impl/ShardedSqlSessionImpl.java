/*
 * @(#)ShardedSqlSessionImpl.java 2012-8-1 下午10:00:00
 *
 * Copyright (c) 2011-2012 Makersoft.org all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 */
package org.makersoft.shards.session.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.makersoft.shards.Shard;
import org.makersoft.shards.ShardId;
import org.makersoft.shards.ShardImpl;
import org.makersoft.shards.ShardOperation;
import org.makersoft.shards.annotation.MyBatisMapper;
import org.makersoft.shards.id.IdGenerator;
import org.makersoft.shards.rule.Rule;
import org.makersoft.shards.select.impl.AdHocSelectFactoryImpl;
import org.makersoft.shards.select.impl.ShardSelectImpl;
import org.makersoft.shards.session.ShardIdResolver;
import org.makersoft.shards.session.ShardedSqlSession;
import org.makersoft.shards.session.ShardedSqlSessionFactory;
import org.makersoft.shards.strategy.ShardStrategy;
import org.makersoft.shards.strategy.exit.impl.ExitOperationsSelectCollector;
import org.makersoft.shards.strategy.exit.impl.FirstNonNullResultExitStrategy;
import org.makersoft.shards.strategy.resolution.ShardResolutionStrategyData;
import org.makersoft.shards.strategy.resolution.ShardResolutionStrategyDataImpl;
import org.makersoft.shards.utils.*;
import org.springframework.util.StringUtils;

/**
 * @author Feng Kuok
 */
public class ShardedSqlSessionImpl implements ShardedSqlSession, ShardIdResolver {

	private final Log log = LogFactory.getLog(getClass());

	private static ThreadLocal<ShardId> currentSubgraphShardId = new ThreadLocal<ShardId>();

	private final ShardedSqlSessionFactory shardedSqlSessionFactory;

	private final List<Shard> shards;

	private final Map<ShardId, Shard> shardIdsToShards;

	private final ShardStrategy shardStrategy;

    private final List<SqlSessionFactory> sqlSessionFactories;

	// constructor
	public ShardedSqlSessionImpl(ShardedSqlSessionFactory shardedSqlSessionFactory,
			ShardStrategy shardStrategy) {
		this.shardedSqlSessionFactory = shardedSqlSessionFactory;
		this.shards = buildShardListFromSqlSessionFactoryShardIdMap(
				shardedSqlSessionFactory.getSqlSessionFactoryShardIdMap(), this);
		this.shardIdsToShards = buildShardIdsToShardsMap();
		this.shardStrategy = shardStrategy;
        sqlSessionFactories = shardedSqlSessionFactory.getSqlSessionFactories();
	}

	static List<Shard> buildShardListFromSqlSessionFactoryShardIdMap(
			Map<SqlSessionFactory, Set<ShardId>> sqlSessionFactoryShardIdMap,
			ShardIdResolver shardIdResolver) {
		List<Shard> list = Lists.newArrayList();
		for (Map.Entry<SqlSessionFactory, Set<ShardId>> entry : sqlSessionFactoryShardIdMap
				.entrySet()) {
			Shard shard = new ShardImpl(entry.getValue(), entry.getKey());
			list.add(shard);

		}

		return list;
	}

	private Map<ShardId, Shard> buildShardIdsToShardsMap() {
		Map<ShardId, Shard> map = Maps.newHashMap();
		for (Shard shard : shards) {
			for (ShardId shardId : shard.getShardIds()) {
				map.put(shardId, shard);
			}
		}
		return map;
	}

	/**
	 * 
	 */
	private Shard getShardForStatement(String statement, List<Shard> shardsToConsider) {
		//TODO(fengkuok) 此处可做本地缓存
		
		// 首先查找主分区？如果没有再找其他分区？
		for (Shard shard : shardsToConsider) {
			if (shard.getSqlSessionFactory() != null
					&& shard.getMappedStatementNames().contains(statement)) {
				return shard;
			}
		}
		return null;
	}

	private SqlSession getSqlSessionForStatement(String statement, List<Shard> shardsToConsider) {
		Shard shard = getShardForStatement(statement, shardsToConsider);
		if (shard == null) {
			return null;
		}
		return shard.establishSqlSession();
	}

	/**
	 * 将虚拟分区转化为物理分区
	 */
	private List<Shard> shardIdListToShardList(List<ShardId> shardIds) {
		Set<Shard> shards = Sets.newHashSet();
		for (ShardId shardId : shardIds) {
			shards.add(shardIdsToShards.get(shardId));
		}
		return Lists.newArrayList(shards);
	}

	/**
	 * @return 所有物理分区
	 */
	public List<Shard> getShards() {
		return Collections.unmodifiableList(shards);
	}

	@Override
	public SqlSession getSqlSessionForStatement(String statement) {
		return getSqlSessionForStatement(statement, shards);
	}

	@Override
	public ShardId getShardIdForStatementOrParameter(String statement, Object parameter) {
		return getShardIdForStatementOrParameter(statement, parameter, shards);
	}

	@Override
	public ShardId getShardIdForStatementOrParameter(String statement, Object parameter,
			List<Shard> shardsToConsider) {
		// TODO(maxr) optimize this by keeping an identity map of objects to shardId
		Shard shard = getShardForStatement(statement, shardsToConsider);
		if (shard == null) {
			return null;
		} else if (shard.getShardIds().size() == 1) {
			return shard.getShardIds().iterator().next();
		} else {
			IdGenerator idGenerator = shardedSqlSessionFactory.getIdGenerator();
			if (idGenerator != null) {
				return idGenerator.extractShardId(this.extractId(parameter));
			} else {
				// TODO(tomislav): also use shard resolution strategy if it returns only 1 shard;
				// throw this error in config instead of here
				throw new RuntimeException(
						"Can not use virtual sharding with non-shard resolving id gen");
			}
		}
	}

	/**
	 * 通过分区选择策略为对象选择分区
	 * 
	 * @param obj
	 *            对象
	 * @return 逻辑分区
	 */
	private ShardId selectShardIdForNewObject(String statement, Object obj) {
		// if(lockedShardId != null) {
		// return lockedShardId;
		// }
        ShardId shardId = shardStrategy.getShardSelectionStrategy().selectShardIdForNewObject(
                statement, obj);
		// lock has been requested but shard has not yet been selected - lock it in
		// if(lockedShard) {
		// lockedShardId = shardId;
		// }
		log.debug(String.format("Selected shard %s for object of type %s", shardId, obj.getClass()
				.getName()));
		return shardId;
	}

	List<ShardId> selectShardIdsFromShardResolutionStrategyData(ShardResolutionStrategyData srsd) {
		IdGenerator idGenerator = shardedSqlSessionFactory.getIdGenerator();
		if ((idGenerator != null) && (srsd.getId() != null)) {
			//
			return Collections.singletonList(idGenerator.extractShardId(srsd.getId()));
		}
		return shardStrategy.getShardResolutionStrategy()
				.selectShardIdsFromShardResolutionStrategyData(srsd);
	}

	private <T> T applyGetOperation(ShardOperation<T> shardOp, ShardResolutionStrategyData srsd) {
		List<ShardId> shardIds = selectShardIdsFromShardResolutionStrategyData(srsd);
		return shardStrategy.getShardAccessStrategy().<T> apply(
				this.shardIdListToShardList(shardIds),
				shardOp,
				new FirstNonNullResultExitStrategy<T>(),
				new ExitOperationsSelectCollector(new AdHocSelectFactoryImpl(
						srsd.getStatement(), srsd.getParameter(), null, RowBounds.DEFAULT), shardStrategy.getShardReduceStrategy()));
	}

	// implements from SqlSession

	@Override
	public <T> T selectOne(String statement) {
		return this.<T> selectOne(statement, null);
	}

	@Override
	public <T> T selectOne(final String statement, final Object parameter) {
		// 从Resolution策略获取
		List<Shard> potentialShards = determineShardsViaResolutionStrategyWithReadOperation(
				statement, parameter);

		Assert.notNull(potentialShards, "ShardResolutionStrategy returnd value cann't be null");

		return new ShardSelectImpl(potentialShards, new AdHocSelectFactoryImpl(statement,
				parameter, null, null), shardStrategy.getShardAccessStrategy(),
				shardStrategy.getShardReduceStrategy()).<T> getSingleResult();
	}

    public static String guessVitualTableName(String statement, Object parameter) {
        String toHandle = statement.substring(0,statement.lastIndexOf("."));
        Class clz = ReflectionUtils.forName(toHandle, ShardedSqlSessionImpl.class);
        MyBatisMapper mapper = (MyBatisMapper) clz.getAnnotation(MyBatisMapper.class);

        return mapper.entityName();
    }

	@Override
	public <E> List<E> selectList(String statement) {
		return this.<E> selectList(statement, null);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter) {
		return this.<E> selectList(statement, parameter, RowBounds.DEFAULT);
	}

	@Override
	public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
		List<ShardId> shardIds = Lists.newArrayList();
		
		List<Shard> potentialShards = determineShardsViaResolutionStrategyWithReadOperation(
				statement, parameter);
		
		if (potentialShards != null) {
			for (Shard shard : potentialShards) {
				shardIds.addAll(shard.getShardIds());
			}
		} else {
			//
			ShardId shardId = this.getShardIdForStatementOrParameter(statement, parameter);
			shardIds = Lists.newArrayList(shardId);
		}

		Assert.isTrue(!shardIds.isEmpty());

		Assert.notNull(potentialShards, "ShardResolutionStrategy returnd value cann't be null");

		return new ShardSelectImpl(potentialShards, new AdHocSelectFactoryImpl(statement,
				parameter, null, rowBounds), shardStrategy.getShardAccessStrategy(),
				shardStrategy.getShardReduceStrategy()).<E> getResultList();
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
		return this.<K, V> selectMap(statement, null, mapKey);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
		return this.<K, V> selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
	}

	@Override
	public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey,
			RowBounds rowBounds) {
		return new ShardSelectImpl(shards, new AdHocSelectFactoryImpl(statement, parameter, mapKey,
				rowBounds), shardStrategy.getShardAccessStrategy(),
				shardStrategy.getShardReduceStrategy()).getResultMap();
	}

	@Override
	public int insert(String statement) {
		return this.insert(statement, Maps.newHashMap());
	}

	@Override
	public int insert(String statement, Object parameter) {
		ShardId shardId = this.selectShardIdForNewObject(statement, parameter);
		if (shardId == null) {
			shardId = this.getShardIdForStatementOrParameter(statement, parameter);
		}

		Assert.notNull(shardId);

		// 设置当前分区id
		setCurrentSubgraphShardId(shardId);

		log.debug(String.format("Inserting object of type %s to shard %s", parameter.getClass(),
				shardId));

		SqlSession session = shardIdsToShards.get(shardId).establishSqlSession();

		IdGenerator idGenerator = shardedSqlSessionFactory.getIdGenerator();
        Serializable idValue = extractId(parameter);
        if(idValue == null) {
            if (idGenerator != null) {
                //TODO(fengkuok) 生成主键 DB生成主键是用专有session？
                Serializable id = idGenerator.generate(session, parameter);

                log.debug(String
                        .format("Generating id for object %s ,the type of IdGenerator is %s and generated Id is %s.",
                                parameter.getClass(), idGenerator.getClass(), id));

                ParameterUtil.generatePrimaryKey(parameter, id);
            }
        }

		return session.insert(statement, ParameterUtil.resolve(parameter, shardId));
	}

	@Override
	public int update(String statement) {
		return this.update(statement, Maps.newHashMap());
	}

	@Override
	public int update(String statement, Object parameter) {
		List<ShardId> shardIds = Lists.newArrayList();

		List<Shard> potentialShards = determineShardsViaResolutionStrategyWithWriteOperation(
				statement, parameter);

		if (potentialShards != null) {
			for (Shard shard : potentialShards) {
				shardIds.addAll(shard.getShardIds());
			}
		} else {
			//
			ShardId shardId = this.getShardIdForStatementOrParameter(statement, parameter);
			shardIds = Lists.newArrayList(shardId);
		}

		Assert.isTrue(!shardIds.isEmpty());

		int rows = 0;
		for (ShardId shardId : shardIds) {
			rows += shardIdsToShards.get(shardId).establishSqlSession()
					.update(statement, ParameterUtil.resolve(parameter, shardId));
			log.debug(String.format("Updateing object of type %s to shard %s",
					parameter == null ? parameter : parameter.getClass(), shardId));
		}

		return rows;
	}

	List<Shard> determineShardsViaResolutionStrategyWithWriteOperation(String statement,
			Object parameter) {
        List<Shard> rtn = new LinkedList<Shard>();
        List<ShardId> ids =  shardedSqlSessionFactory.getWrite();

        loop:for(ShardId id : ids) {
            for(Shard s : shards) {
                if(s.getShardIds().contains(id)) {
                    rtn.add(s);
                    continue loop;
                }
            }
        }

        return rtn;
	}


	List<Shard> determineShardsViaResolutionStrategyWithReadOperation(String statement,
			Object parameter) {
        String entityName = guessVitualTableName(statement, parameter);
		return this.determineShardsObjectsViaResolutionStrategy(statement, parameter, null, entityName);
	}

	/**
	 * 通过statement和parameter确定分区 如果parameter中可以提取出主键ID,首先通过ID去确定唯一分区
	 */
	private List<Shard> determineShardsObjectsViaResolutionStrategy(String statement,
			Object parameter, Serializable id, String entityName) {
		ShardResolutionStrategyData srsd = new ShardResolutionStrategyDataImpl(statement,
				parameter, id, entityName);
		List<ShardId> shardIds = this.selectShardIdsFromShardResolutionStrategyData(srsd);
		return shardIdListToShardList(shardIds);
	}

	/**
	 * 获取对象主键值
	 */
	public static Serializable extractId(Object obj) {
		if (obj != null) {
			if (obj instanceof String || obj instanceof Number) {
				// 当参数为Number/String类型时是否可以认为是主键？
				return (Serializable) obj;
			} else if(obj instanceof Map) {
                Map paramMap = (Map)obj;
                return (Serializable)paramMap.get("id");
            }
			return ParameterUtil.extractPrimaryKey(obj);
		}
		return null;
	}

    public static String extractVitualName(Object obj) {
        return ParameterUtil.extractVirtualTableName(obj);
    }

	@Override
	public int delete(String statement) {
		return delete(statement, Maps.newHashMap());
	}

	@Override
	public int delete(String statement, Object parameter) {
		List<ShardId> shardIds = Lists.newArrayList();

		List<Shard> potentialShards = determineShardsViaResolutionStrategyWithWriteOperation(
				statement, parameter);
		if (potentialShards != null) {
			for (Shard shard : potentialShards) {
				shardIds.addAll(shard.getShardIds());
			}
		} else {
			// 此种情况下按先从主分区查询statement如果不存在则查询全部分区来定位
			ShardId shardId = this.getShardIdForStatementOrParameter(statement, parameter);
			shardIds = Lists.newArrayList(shardId);
		}

		Assert.isTrue(!shardIds.isEmpty());

		int rows = 0;
		for (ShardId shardId : shardIds) {
			rows += shardIdsToShards.get(shardId).establishSqlSession()
					.delete(statement, ParameterUtil.resolve(parameter, shardId));
			log.debug(String.format("Deleting object of type %s to shard %s", parameter, shardId));
		}
		return rows;
	}

	@Override
	public void commit() {
		commit(false);
	}

	@Override
	public void commit(boolean force) {
//		throw new UnsupportedOperationException(
//				"Manual commit is not allowed over a Spring managed SqlSession");
//		for (Shard shard : this.getShards()) {
//			SqlSession session = shard.getSqlSession();
//			if (session != null) {
//				session.commit(force);
//			}
//		}
	}

	@Override
	public void rollback() {
		rollback(false);
	}

	@Override
	public void rollback(boolean force) {
//		for (Shard shard : this.getShards()) {
//			SqlSession session = shard.getSqlSession();
//			if (session != null) {
//				session.rollback(force);
//			}
//		}
	}

	@Override
	public List<BatchResult> flushStatements() {
		return null;
	}

	@Override
	public void close() {
//		for (Shard shard : this.getShards()) {
//			SqlSession session = shard.getSqlSession();
//			if (session != null) {
//				session.close();
//			}
//		}
	}

	@Override
	public void clearCache() {
		for (Shard shard : this.getShards()) {
			SqlSession session = shard.establishSqlSession();
			if (session != null) {
				session.clearCache();
			}
		}
	}

	@Override
	public <T> T getMapper(Class<T> type) {
		for (Shard shard : this.getShards()) {
			if (shard.hasMapper(type)) {
				return shard.establishSqlSession().getMapper(type);
			}
		}

		throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
	}

	@Override
	public void select(String statement, ResultHandler handler) {
		throw new UnsupportedOperationException(
				"opration select is not allowed over a ShardedSqlSession");
	}

	@Override
	public void select(String statement, Object parameter, ResultHandler handler) {
		throw new UnsupportedOperationException(
				"opration select is not allowed over a ShardedSqlSession");
	}

	@Override
	public void select(String statement, Object parameter, RowBounds rowBounds,
			ResultHandler handler) {
		throw new UnsupportedOperationException(
				"opration select is not allowed over a ShardedSqlSession");
	}

	@Override
	public Configuration getConfiguration() {
		throw new UnsupportedOperationException(
				"Manual get configuration is not allowed over a Spring managed SqlSession");
	}

	@Override
	public Connection getConnection() {
		throw new UnsupportedOperationException(
				"Manual get connection is not allowed over a Spring managed SqlSession");
	}

	// ~~~~~~~~~~~~~~~
	public static ShardId getCurrentSubgraphShardId() {
		return currentSubgraphShardId.get();
	}

	public static void setCurrentSubgraphShardId(ShardId shardId) {
		currentSubgraphShardId.set(shardId);
	}

}
