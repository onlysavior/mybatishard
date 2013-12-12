/*
 * @(#)SequenceBolckIdGenerator.java 2012-8-1 下午10:00:00
 *
 * Copyright (c) 2011-2012 Makersoft.org all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 */
package org.makersoft.shards.id.db;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.session.SqlSession;
import org.makersoft.shards.ShardId;
import org.makersoft.shards.id.IdGenerator;

/**
 *
 */
public class SequenceBolckIdGenerator implements IdGenerator {
    private Lock lock = new ReentrantLock();
    private SequenceIdDao sequenceIdDao;
    private String name;

    private volatile IdBlock currentIdBlock = null;

    public void setSequenceIdDao(SequenceIdDao sequenceIdDao) {
        this.sequenceIdDao = sequenceIdDao;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public synchronized Serializable generate(SqlSession session, Object object) {
        Long rtn = -1L;
        if (currentIdBlock == null) {
            try {
                lock.lock();
                currentIdBlock = sequenceIdDao.nextIdBlock(name);
                rtn = currentIdBlock.getAndIncrement();
            } catch (Exception ignored) {
                System.out.println(ignored);
            } finally {
                lock.unlock();
            }
        }

        rtn = currentIdBlock.getAndIncrement();
        if(rtn < 0) {
            try {
                lock.lock();
                currentIdBlock = sequenceIdDao.nextIdBlock(name);
                rtn = currentIdBlock.getAndIncrement();
            } catch(Exception ignored) {

            } finally {
                lock.unlock();
            }
        }

        if(rtn < 0) {
            throw new IllegalStateException("overflow or SQL Execption happend nameColum is "+ name);
        }

        return rtn;
    }

    @Override
    public ShardId extractShardId(Serializable identifier) {
        throw new UnsupportedOperationException();
    }
}
