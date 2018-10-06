package org.jmicro.example.test;

import java.sql.SQLException;

import org.jmicro.api.client.AbstractServiceProxy;
import org.jmicro.api.client.ServiceInvocationHandler;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Constants;
import org.jmicro.common.Utils;
import org.jmicro.example.api.ITestRpcService;
import org.jmicro.objfactory.simple.SimpleObjectFactory;
import org.junit.Test;

public class TestRpcRequest {

	@Test
	public void testRpcRequest() throws SQLException {
		 
		 RpcRequest req = new RpcRequest();
		 //req.setSession();
		 
		 //req.setImpl("org.jmicro.objfactory.simple.TestRpcService");
		 req.setServiceName("org.jmicro.objfactory.simple.ITestRpcService");
		 req.setMethod("hello");
		 req.setArgs(new Object[]{"Yyl"});
		 req.setNamespace("test");
		 req.setVersion("1.0.0");
		 req.setRequestId(1000L);
		 
		// JmicroManager.getIns().addRequest(req);
		 Utils.waitForShutdown();
	}
	
/*	@Test
	public void testDynamicProxy() {
		ITestRpcService src = SimpleObjectFactory.createDynamicServiceProxy(
				ITestRpcService.class,Constants.DEFAULT_NAMESPACE,Constants.DEFAULT_VERSION);
		AbstractServiceProxy asp = (AbstractServiceProxy)src;
		asp.setHandler(new ServiceInvocationHandler());
		System.out.println(src.hello("Hello"));
		System.out.println("testDynamicProxy");
	}*/
	
	@Test
	public void testStartServer() {
		/*Config cfg = new Config();
		cfg.setBindIp("localhost");
		cfg.setPort(9800);;
		cfg.setBasePackages(new String[]{"org.jmicro","org.jmtest"});
		cfg.setRegistryUrl(new URL("zookeeper","localhost",2180));
		JMicroContext.setCfg(cfg);*/
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		Utils.waitForShutdown();
	}

}
