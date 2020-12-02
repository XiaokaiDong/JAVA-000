package jdbc.aop;

import annotations.ReadOnly;
import jdbc.datasource.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class DataSourceAspect {
    @Pointcut("@annotation(annotations.ReadOnly)")
    public void dataSourcePoint() {

    }

    @Around("dataSourcePoint()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        ReadOnly readOnly = method.getAnnotation(ReadOnly.class);
        if (readOnly == null) {
            DynamicDataSource.setReadOnly(false);
            log.debug("set readwrite datasource");
        } else {
            DynamicDataSource.setReadOnly(readOnly.readonly());
            log.debug("set readonly datasource? " + readOnly.readonly());
        }

        try{
            return point.proceed();
        }finally {
            DynamicDataSource.setReadOnly(false);
            log.debug("fallback to readwrite datasource");
        }
    }
}
