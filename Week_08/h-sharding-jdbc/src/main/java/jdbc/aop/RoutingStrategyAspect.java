package jdbc.aop;


import jdbc.datasource.readwrite.MultiDataSources;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import annotations.RoutingStrategy;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RoutingStrategyAspect {
    @Pointcut("@annotation(annotations.RoutingStrategy)")
    public void routingPointCut() {

    }

    @Around("routingPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        RoutingStrategy strategy = method.getAnnotation(RoutingStrategy.class);
        if (strategy == null) {
            MultiDataSources.setRoutingStrategy("TRIVIAL");
            log.debug("set routing strategy [TRIVIAL]");
        } else {
            MultiDataSources.setRoutingStrategy(strategy.name());
            log.debug("set routing strategy [ " + strategy.name() + " ]");
        }

        try {
            return point.proceed();
        }finally {
            log.debug("routing aop error!");
        }
    }
}
