/*
 * @(#)ShardedSqlSessionFactoryBean.java 2012-8-1 下午10:00:00
 *
 * Copyright (c) 2011-2012 Makersoft.org all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 */
package org.makersoft.shards.spring;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.makersoft.shards.ShardedConfiguration;
import org.makersoft.shards.cfg.ShardConfiguration;
import org.makersoft.shards.cfg.impl.ShardConfigurationImpl;
import org.makersoft.shards.datasource.ShardAtomDataSource;
import org.makersoft.shards.feature.Feature;
import org.makersoft.shards.feature.FeatureEnum;
import org.makersoft.shards.id.IdGenerator;
import org.makersoft.shards.plugin.RuleBaseStatementInterceptor;
import org.makersoft.shards.session.ShardedSqlSessionFactory;
import org.makersoft.shards.strategy.ShardStrategyFactory;
import org.makersoft.shards.utils.Assert;
import org.makersoft.shards.utils.StringUtil;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
public class ShardedSqlSessionFactoryBean implements
        FactoryBean<ShardedSqlSessionFactory>, InitializingBean {

    private Resource configLocation;

    private Resource[] mapperLocations;

    private Map<Integer, DataSource> dataSources;

    private Properties configurationProperties;

    private String environment = ShardedSqlSessionFactory.class.getSimpleName();

//	private boolean failFast;

    private Interceptor[] plugins;

    private TypeHandler<?>[] typeHandlers;

    private String typeHandlersPackage;

    private Class<?>[] typeAliases;

    private String typeAliasesPackage;

    private ShardedSqlSessionFactory shardedSqlSessionFactory;

    private ShardStrategyFactory shardStrategyFactory;

    private IdGenerator idGenerator;

    //直接配置ShardConfiguration
    private List<ShardConfigurationImpl> shardConfigurations;

    private RuleBean ruleBean;

    //read or write datasource
    private CopyOnWriteArraySet<Integer> writeDataSource = new CopyOnWriteArraySet<Integer>();
    private CopyOnWriteArraySet<Integer> readDataSource = new CopyOnWriteArraySet<Integer>();

    private Feature feature;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (plugins == null) {
            plugins = new Interceptor[1];
            plugins[0] = new RuleBaseStatementInterceptor(ruleBean);
        } else {
            Interceptor[] newPlugins = new Interceptor[plugins.length + 1];
            System.arraycopy(plugins, 0, newPlugins, 0, plugins.length);
            newPlugins[newPlugins.length - 1] = new RuleBaseStatementInterceptor(ruleBean);
        }

        List<ShardConfiguration> shardConfigs = new ArrayList<ShardConfiguration>();

        if (CollectionUtils.isEmpty(shardConfigurations)) {
            for (Map.Entry<Integer, DataSource> entry : dataSources.entrySet()) {
                int shardId = entry.getKey();    //虚拟分区ID
                String type = null;
                DataSource dataSource = entry.getValue();    //虚拟分区所属数据源

                if (dataSource instanceof ShardAtomDataSource) {
                    feature.on(FeatureEnum.READ_WRITE_SPLIT.getIndex());
                    ShardAtomDataSource atomDataSource = (ShardAtomDataSource) dataSource;
                    type = atomDataSource.getType();

                    if (type.contains(ShardAtomDataSource.READ_TYPE)) {
                        readDataSource.add(shardId);
                    }

                    if (type.contains(ShardAtomDataSource.WRITE_TYPE)) {
                        writeDataSource.add(shardId);
                    }
                }

                SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
                factoryBean.setConfigLocation(this.configLocation);
                factoryBean.setMapperLocations(this.mapperLocations);
                factoryBean.setDataSource(dataSource);
                factoryBean.setEnvironment(this.environment);
                factoryBean.setConfigurationProperties(this.configurationProperties);
                factoryBean.setPlugins(this.plugins);
                factoryBean.setTypeHandlers(this.typeHandlers);
                factoryBean.setTypeHandlersPackage(this.typeHandlersPackage);
                factoryBean.setTypeAliases(this.typeAliases);
                factoryBean.setTypeAliasesPackage(this.typeAliasesPackage);

                SqlSessionFactory sessionFacotry = factoryBean.getObject();

                shardConfigs.add(new ShardConfigurationImpl(shardId, dataSource, sessionFacotry));
            }
        } else {
            for (ShardConfigurationImpl shardConfiguration : shardConfigurations) {

                Assert.notNull(shardConfiguration.getShardId(), "shard id can not be null.");
                Assert.notNull(shardConfiguration.getShardDataSource(), "data source can not be null.");

                if(!StringUtil.isEmpty(shardConfiguration.getType())) {
                    feature.on(FeatureEnum.READ_WRITE_SPLIT.getIndex());
                    String type = shardConfiguration.getType();
                    ShardAtomDataSource dataSource = (ShardAtomDataSource)shardConfiguration.getShardDataSource();

                    if (type.contains(ShardAtomDataSource.READ_TYPE)) {
                        readDataSource.add(shardConfiguration.getShardId());
                    }

                    if (type.contains(ShardAtomDataSource.WRITE_TYPE)) {
                        writeDataSource.add(shardConfiguration.getShardId());
                    }
                }

                SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
                factoryBean.setConfigLocation(shardConfiguration.getConfigLocation());
                factoryBean.setMapperLocations(shardConfiguration.getMapperLocations());
                factoryBean.setDataSource(shardConfiguration.getShardDataSource());
                factoryBean.setEnvironment(this.environment);
                factoryBean.setConfigurationProperties(this.configurationProperties);
                factoryBean.setPlugins(this.plugins);
                factoryBean.setTypeHandlers(this.typeHandlers);
                factoryBean.setTypeHandlersPackage(shardConfiguration.getTypeHandlersPackage());
                factoryBean.setTypeAliases(this.typeAliases);
                factoryBean.setTypeAliasesPackage(shardConfiguration.getTypeAliasesPackage());

                SqlSessionFactory sessionFacotry = factoryBean.getObject();
                shardConfiguration.setSqlSessionFactory(sessionFacotry);

                shardConfigs.add(shardConfiguration);
            }

        }

        //check RW split
        if(feature.isOn(FeatureEnum.READ_WRITE_SPLIT.getIndex())) {
            if(readDataSource.size() < 1) {
                throw new IllegalArgumentException("Read DataSource size < 1");
            }

            if(writeDataSource.size() > 1) {
                throw new IllegalArgumentException("current not support muti write datasource");
            }
        }

        ShardedConfiguration configuration = new ShardedConfiguration(shardConfigs, this.shardStrategyFactory,
                idGenerator, readDataSource, writeDataSource);
        shardedSqlSessionFactory = configuration.buildShardedSessionFactory();
    }

    @Override
    public ShardedSqlSessionFactory getObject() throws Exception {
        if (this.shardedSqlSessionFactory == null) {
            afterPropertiesSet();
        }

        return this.shardedSqlSessionFactory;
    }

    @Override
    public Class<?> getObjectType() {
        return this.shardedSqlSessionFactory == null ? ShardedSqlSessionFactory.class
                : this.shardedSqlSessionFactory.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    //Setter from here
    public void setDataSources(final Map<Integer, DataSource> dataSources) {
        this.dataSources = dataSources;
    }

    public void setConfigLocation(Resource configLocation) {
        this.configLocation = configLocation;
    }

    public void setMapperLocations(Resource[] mapperLocations) {
        this.mapperLocations = mapperLocations;
    }

    public void setConfigurationProperties(Properties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }


    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setPlugins(Interceptor[] plugins) {
        this.plugins = plugins;
    }

    public void setTypeHandlers(TypeHandler<?>[] typeHandlers) {
        this.typeHandlers = typeHandlers;
    }

    public void setTypeHandlersPackage(String typeHandlersPackage) {
        this.typeHandlersPackage = typeHandlersPackage;
    }

    public void setTypeAliases(Class<?>[] typeAliases) {
        this.typeAliases = typeAliases;
    }

    public void setTypeAliasesPackage(String typeAliasesPackage) {
        this.typeAliasesPackage = typeAliasesPackage;
    }

    public void setShardStrategyFactory(ShardStrategyFactory shardStrategyFactory) {
        this.shardStrategyFactory = shardStrategyFactory;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public void setShardConfigurations(List<ShardConfigurationImpl> shardConfigurations) {
        this.shardConfigurations = shardConfigurations;
    }

    public void setRuleBean(RuleBean ruleBean) {
        this.ruleBean = ruleBean;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }
}
