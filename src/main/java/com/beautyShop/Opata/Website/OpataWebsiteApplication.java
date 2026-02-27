package com.beautyShop.Opata.Website;

import com.beautyShop.Opata.Website.Config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OpataWebsiteApplication {

	public static void main(String[] args) {
		EnvLoader.load();

		SpringApplication.run(OpataWebsiteApplication.class, args);
	}

}
