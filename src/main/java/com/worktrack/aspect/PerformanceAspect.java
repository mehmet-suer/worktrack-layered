package com.worktrack.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    // This method targets all methods of all classes under the com.worktrack.service package.
    // (*) represents any return type.
    // com.worktrack.service..* covers all subpackages as well.
    // If you only used com.worktrack.service.*, it would match classes directly under service without subpackages.

    /*
        (..)     Any number of parameters of any type
        (*)      Exactly one parameter, any type
        (String) Exactly one parameter of type String
        (*, *)   Exactly two parameters, any types
        (String, int) Exactly two parameters: first String, second int
        @Around("execution(* com.worktrack.service..*.exec*(..))")
                   All methods starting with 'exec' in any class under com.worktrack.service and its subpackages
    */
    @Around("execution(* com.worktrack.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // execute method
        long duration = System.currentTimeMillis() - start;

        logger.info("{} executed in {} ms", joinPoint.getSignature(), duration);
        return result;
    }
}


