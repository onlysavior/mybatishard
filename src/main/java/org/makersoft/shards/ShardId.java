/*
 * @(#)ShardId.java 2012-8-1 下午10:00:00
 *
 * Copyright (c) 2011-2012 Makersoft.org all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 */
package org.makersoft.shards;

import java.util.List;

import org.makersoft.shards.utils.Assert;
import org.makersoft.shards.utils.StringUtil;

/**
 * Uniquely identifies a virtual shard
 * 
 */
public class ShardId {
	
	private static final String SPLIT = "_";

	private final int shardId;
	
	//constractor
	public ShardId(int shardId) {
		this.shardId = shardId;
	}


    public final int getId() {
		return shardId;
	}

	public static ShardId findByShardId(List<ShardId> shardIds, int id){
		for(ShardId shardId : shardIds) {
			if (shardId.getId() == id){
				return shardId;
			}
		}
		
		throw new MyBatisShardsException("Not found shard id {" + id +"}");
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + shardId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ShardId other = (ShardId) obj;
		if (shardId != other.shardId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(shardId);
	}


}
