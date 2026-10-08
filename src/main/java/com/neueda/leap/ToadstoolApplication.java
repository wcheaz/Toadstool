package com.neueda.leap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.neueda.leap.mapper")
@EnableScheduling
public class ToadstoolApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToadstoolApplication.class, args);
	}
}
