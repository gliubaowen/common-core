package com.ibm.common.core.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

/**
 * 切面日志配置
 * <p>
 * 使用fastjson解析参数
 * </p>
 * 
 * @author LiuBaoWen
 *
 */
@Aspect
@Component
public class LogAspect {

	private Logger logger = LoggerFactory.getLogger(LogAspect.class);

	private long startTimeMillis = 0;

	private long endTimeMillis = 0;

	private long exceptionTimeMillis = 0;

	@Pointcut("execution(* com.ibm.common..*(..))")
	public void logPointcut() {
	}

	@Before("logPointcut()")
	public void doBefore(JoinPoint joinPoint) {
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();
		Object[] args = joinPoint.getArgs();
		startTimeMillis = System.currentTimeMillis();
		logger.info("start execute {}#{}, time:{}, args:{}", className, methodName, startTimeMillis,
				JSON.toJSONString(args));
	}

//	@After("logPointcut()")
	public void doAfter(JoinPoint joinPoint) {
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();
		logger.info("end execute {}#{}, time:{}", className, methodName, System.currentTimeMillis());
	}

	@AfterReturning(pointcut = "logPointcut()", returning = "result")
	public void doAfterReturning(JoinPoint joinPoint, Object result) {
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();
		endTimeMillis = System.currentTimeMillis();
		logger.info("end execute {}#{}, time:{}, execute time:{}, execute result:{}", className, methodName,
				endTimeMillis, endTimeMillis - startTimeMillis, JSON.toJSONString(result));
	}

	@AfterThrowing(pointcut = "logPointcut()", throwing = "ex")
	public void doAfterThrowing(JoinPoint joinPoint, Exception ex) {
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = joinPoint.getSignature().getName();
		exceptionTimeMillis = System.currentTimeMillis();
		logger.error("execute {}#{}, throws Exception, time:{}, execute time:{}, msg:{}, err:{}", className, methodName,
				exceptionTimeMillis, exceptionTimeMillis - startTimeMillis, ex.getMessage(), ex.getStackTrace());
	}

}
