package org.alexmond.sample;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
// A sample controller, not a test class; the name only starts with "Test".
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestController {

	@GetMapping("/")
	public String helloWorld() {
		return "Hello world";
	}

}
