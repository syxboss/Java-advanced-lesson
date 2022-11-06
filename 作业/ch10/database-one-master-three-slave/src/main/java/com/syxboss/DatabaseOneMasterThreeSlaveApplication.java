package com.syxboss;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.syxboss.dao")
public class DatabaseOneMasterThreeSlaveApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatabaseOneMasterThreeSlaveApplication.class, args);
	}

}
