package org.makersoft.shards.domain;

import org.makersoft.shards.annotation.PrimaryKey;
import org.makersoft.shards.annotation.Table;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: yanye.lj
 * Date: 13-12-10
 * Time: ÏÂÎç12:06
 * To change this template use File | Settings | File Templates.
 */
@Table(value = "user")
public class RuleUser implements Serializable {
    @PrimaryKey
    private Integer id;
    private String name;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
