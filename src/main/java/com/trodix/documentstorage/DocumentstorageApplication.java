package com.trodix.documentstorage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
public class DocumentstorageApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentstorageApplication.class, args);
	}

}
