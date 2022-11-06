package com.syxboss.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t")
public class Human {
    @TableId(type = IdType.AUTO)
    private Long id ;
    @TableField("c")
    private String username ;
}
