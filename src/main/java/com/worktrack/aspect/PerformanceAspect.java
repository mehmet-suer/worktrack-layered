package com.worktrack.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    // Bu metot, com.worktrack.service paketindeki tüm sınıfların tüm metotlarını hedef alır
    // (* tum method tiplerini temsil eder)
    // com.worktrack.service..* tüm alt paketleri de kapsar, sadece service olsaydi com.worktrack.service.* olacaktı.
    /*
        (..)	Her sayıda, her tipte parametre
        (*)	Sadece 1 parametre, tipi fark etmez
        (String)	Sadece tek String parametre
        (*, *)	2 parametre, her tip olabilir
        (String, int)	Sadece String ve int sırasıyla
        @Around("execution(* com.worktrack.service..*.exec*(..))") exec ile baslayan tüm methodlar

     */
    @Around("execution(* com.worktrack.service..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed(); // methodu çalıştır
        long duration = System.currentTimeMillis() - start;

        System.out.println(joinPoint.getSignature() + " süresi: " + duration + " ms");
        return result;
    }
}


