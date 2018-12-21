package org.jmicro.example.provider;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.SBreakingRule;
import org.jmicro.api.annotation.SMethod;
import org.jmicro.api.annotation.Service;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.common.Constants;
import org.jmicro.example.api.ISayHello;

@Service(maxSpeed=100,baseTimeUnit=Constants.TIME_SECONDS)
@Component
public class SayHelloImpl implements ISayHello {

	@Override
	@SMethod(
		//breakingRule="1S 50% 500MS",
		//1秒钟内异常超50%，熔断服务，熔断后每80毫秒做一次测试
		breakingRule = @SBreakingRule(enable=true,breakTimeInterval=1000,percent=50,checkInterval=80),
		loggable=1,	
		testingArgs="AgATW0xqYXZhLmxhbmcuT2JqZWN0OwH/9AABAf/zAApBcmUgeW91IE9L",//测试参数
		monitorEnable=1,
		timeWindow=20,//统计时间窗口20S
		checkInterval=2,//采样周期2S
		baseTimeUnit="S"
	)
	public String hello(String name) {
		if(SF.isLoggable(true,MonitorConstant.LOG_DEBUG)) {
			SF.doBussinessLog(MonitorConstant.LOG_DEBUG,SayHelloImpl.class,null, name);
		}
		//System.out.println("Server hello: " +name);
		return "Server say hello to: "+name;
	}

	
}
