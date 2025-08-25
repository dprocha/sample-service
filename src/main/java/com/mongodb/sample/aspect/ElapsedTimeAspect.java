package com.mongodb.sample.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ElapsedTimeAspect {

    // Define a pointcut for methods to be intercepted. In this case, all methods in any package of your application.
    @Pointcut("execution(* com.mongodb.uuid.generator.sizer.finance.service..*(..))")
    // Adjust the package path as needed
    public void trackElapsedTime() {
    }

    // Before advice to capture the start time
    @Before("trackElapsedTime()")
    public void beforeMethodExecution() {
        long startTime = System.currentTimeMillis();
        // Store start time in thread-local or custom context (e.g., request scope) if needed
        ElapsedTime.setStartTime(startTime); // Using ExecutionContext class to hold the start time
    }

    // After advice to calculate the elapsed time
    @After("trackElapsedTime()")
    public void afterMethodExecution() {
        long endTime = System.currentTimeMillis();
        long startTime = ElapsedTime.getStartTime(); // Retrieve start time
        long elapsedTime = endTime - startTime;
        double seconds = elapsedTime / 1000.0;

        // Log the elapsed time
        log.info("Elapsed time: {} seconds", seconds);
    }
}
