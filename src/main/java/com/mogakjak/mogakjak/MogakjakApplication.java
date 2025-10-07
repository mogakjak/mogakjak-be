package com.mogakjak.mogakjak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MogakjakApplication {

	public static void main(String[] args) {
		SpringApplication.run(MogakjakApplication.class, args);
	}

}
