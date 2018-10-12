package org.jmicro.example.provider;

import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.servicemanager.ComponentManager;
import org.jmicro.common.Utils;

public class ServiceProvider {

	public static void main(String[] args) {
		
		Config.parseArgs(args);
		
		IObjectFactory of = ComponentManager.getObjectFactory();
		of.start();
		
		Utils.getIns().waitForShutdown();
	}

}