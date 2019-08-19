package com.ideabaker.samples.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@SpringBootApplication
public class CircuitbreakerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CircuitbreakerApplication.class, args);
	}

	@Bean
	ReactiveCircuitBreakerFactory circuitBreakerFactory() {
		var factory = new ReactiveResilience4JCircuitBreakerFactory();
		factory.configureDefault(name -> new Resilience4JConfigBuilder(name)
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build())
				.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				.build());
		return factory;
	}

}

@Slf4j
@RestController
class FailingController {
	private final FailingService failingService;
	private final ReactiveCircuitBreaker circuitBreaker;

	FailingController(FailingService failingService, @Qualifier("circuitBreakerFactory") ReactiveCircuitBreakerFactory cbf) {
		this.failingService = failingService;
		this.circuitBreaker = cbf.create("greet");
	}

	@GetMapping("/greet")
	Publisher<String> greet(@RequestParam(required = false) String name) {
		var results = failingService.greet(name); //cold

		return this.circuitBreaker.run(results, throwable -> {
            log.warn("error case! '{}'", throwable.getLocalizedMessage());
		    return Mono.just("Hello world!");
        });
	}
}

@Slf4j
@Service
class FailingService {
	Mono<String> greet(String name) {
		var seconds = (long) (Math.random() * 10);
		return Optional.ofNullable(name)
				.map(n -> {
                    String str = "Hello " + n + "(in " + seconds + " seconds)" + "!";
                    log.info(str);
                    return Mono.just(str);
                })
				.orElse(Mono.error(new IllegalArgumentException()))
				.delayElement(Duration.ofSeconds(seconds));
	}
}
