package org.jmicro.main;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Reference;

@Component
public class TestRpcClient {

	@Reference(required=true)
	private ITestRpcService rpcService;
	
	public void invokeRpcService(){
		String result = rpcService.hello("Hello RPC Server");
		System.out.println("Get remote result:"+result);
	}
	
	public void invokePersonService(){
		Persion p = new Persion();
		p.setId(1234);
		p.setUsername("Client person Name");
		p = rpcService.getPerson(p);
		System.out.println(p.toString());
	}
	
}
