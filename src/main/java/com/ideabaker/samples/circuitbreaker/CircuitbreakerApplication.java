package com.ideabaker.samples.circuitbreaker;

import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@SpringBootApplication
public class CircuitbreakerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CircuitbreakerApplication.class, args);
	}

}

@RestController
class FailingController {
	private final FailingService failingService;
	private final ReactiveCircuitBreaker circuitBreaker;

	FailingController(FailingService failingService, ReactiveCircuitBreakerFactory cbf) {
		this.failingService = failingService;
		this.circuitBreaker = cbf.create("greet");
	}

	@GetMapping("/greet")
	Publisher<String> greet(@RequestParam(required = false) String name) {
		var results = failingService.greet(name); //cold

		return this.circuitBreaker.run(results, throwable -> Mono.just("Hello world!"));
	}
}

@Service
class FailingService {
	Mono<String> greet(String name) {
		return Optional.ofNullable(name)
				.map(n -> Mono.just("Hello " + n + "!"))
				.orElse(Mono.error(new IllegalArgumentException()));
	}
}
