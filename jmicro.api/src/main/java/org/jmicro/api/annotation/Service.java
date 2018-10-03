package org.jmicro.api.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jmicro.common.Constants;

@Target({TYPE})
@Retention(RUNTIME)
public @interface Service {

	public String value() default "";
	
	public String registry() default Constants.DEFAULT_REGISTRY;
	
	//public int retryCount() default 3;
	
	public String server() default Constants.DEFAULT_SERVER;
	
	public Class<?>[] interfaces() default {};
	
	public String namespace() default Constants.DEFAULT_NAMESPACE;
	
	public String version() default Constants.DEFAULT_VERSION;
	
	public int retryInterval() default 500;
	
	//method must can be retry, or 1
	public int retryCnt() default 3;
	
	public int timeout() default 2000;
	
	public int maxFailBeforeDowngrade() default 100;
	
	public int maxFailBeforeCutdown() default 500;
	
	public String testingArgs() default "";
	
	/**
	 * max qps
	 */
	public int maxSpeed() default -1;
	
	/**
	 * min qps
	 * real qps less this value will downgrade service
	 */
	public int minSpeed() default -1;
	
	/**
	 *  milliseconds
	 *  speed up when real response time less avgResponseTime, 
	 *  speed down when real response time less avgResponseTime
	 *  
	 */
	public int avgResponseTime() default -1;
	
}
