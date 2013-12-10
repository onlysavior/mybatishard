/*
 * @(#)ShardResolutionStrategyDataImpl.java 2012-8-1 下午10:00:00
 *
 * Copyright (c) 2011-2012 Makersoft.org all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 */
package org.makersoft.shards.strategy.resolution;

import org.makersoft.shards.rule.Rule;

import java.io.Serializable;

/**
 * 
 */
public class ShardResolutionStrategyDataImpl implements ShardResolutionStrategyData {
	
	private final String statement;
	
	private final Object parameter;
	
	private final Serializable id;

    private String entityName;


	public ShardResolutionStrategyDataImpl(String statement, Object parameter, Serializable id){
		this.statement = statement;
		this.parameter = parameter;
		this.id = id;
	}

    public ShardResolutionStrategyDataImpl(String statement, Object parameter, Serializable id,
                                           String vitualTableName) {
        this(statement, parameter, id);
        this.entityName = vitualTableName;
    }

	@Override
	public String getStatement() {
		return statement;
	}

	@Override
	public Object getParameter() {
		return parameter;
	}

	@Override
	public Serializable getId() {
		return id;
	}

    @Override
    public String getEntityName() {
        return entityName;
    }

}
