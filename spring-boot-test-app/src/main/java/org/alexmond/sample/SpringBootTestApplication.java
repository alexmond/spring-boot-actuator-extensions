package org.alexmond.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Instantiated by Spring as a configuration class, so not a utility class.
@SuppressWarnings("PMD.UseUtilityClass")
public class SpringBootTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootTestApplication.class, args);
	}

}
