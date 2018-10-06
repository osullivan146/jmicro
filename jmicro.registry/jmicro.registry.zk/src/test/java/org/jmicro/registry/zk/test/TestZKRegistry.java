package org.jmicro.registry.zk.test;

import org.jmicro.api.Config;
import org.jmicro.common.Utils;
import org.jmicro.registry.zk.ZKRegistry;
import org.jmicro.zk.ZKDataOperator;
import org.junit.Test;

public class TestZKRegistry {
	
	/**
	 * 
	org.jmicro.example.api.ITestRpcService####host%3D192.168.3.3%26port%3D59429
	%26namespace%3DdefaultNamespace%26version%3D0.0.0%26time%3D362551174074939239
	 */
	@Test
	public void testServiceAdd() {
		Config.parseArgs(new String[0]);
		ZKRegistry r = new ZKRegistry();
		r.init();
		
		r.addServiceListener("org.jmicro.example.api.ITestRpcService##defaultNamespace##0.0.0", 
				(type,si)->{
			System.out.println(type);
			System.out.println(si.val());
		});
		
		Utils.waitForShutdown();
	}
	
	@Test
	public void testNodeCreate() {
		Config.parseArgs(new String[0]);
		ZKDataOperator.getIns().addNodeListener("/jmicro/config/test",
			(type,path,data)->{
				System.out.println(type);
				System.out.println(path);
				System.out.println(data);
			});
		Utils.waitForShutdown();
	}
	
}
