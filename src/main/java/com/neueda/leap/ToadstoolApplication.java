package com.neueda.leap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.neueda.leap.mapper")
public class ToadstoolApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToadstoolApplication.class, args);
	}
}
