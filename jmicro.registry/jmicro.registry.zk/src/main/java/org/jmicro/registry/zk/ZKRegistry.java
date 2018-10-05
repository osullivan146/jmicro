/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmicro.registry.zk;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.StateListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.zkclient.ZkclientZookeeperTransporter;
import org.jmicro.api.Config;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Lazy;
import org.jmicro.api.annotation.Registry;
import org.jmicro.api.exception.FusingException;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:09
 */
@Registry(Constants.DEFAULT_REGISTRY)
@Lazy(false)
public class ZKRegistry implements IRegistry {

	private final static Logger logger = LoggerFactory.getLogger(ZKRegistry.class);
	
	private ZookeeperClient client = null;
	
	private Map<String,Set<ServiceItem>> serviceItems = new ConcurrentHashMap<String,Set<ServiceItem>>();
	
	@Override
	@JMethod("init")
	public void init() {
		URL url = Config.getRegistryUrl();
		this.client = new ZkclientZookeeperTransporter().connect(url);
		
		//this.client = new CuratorZookeeperTransporter().connect(url);
		this.client.addStateListener((state)->{
			if(StateListener.CONNECTED == state) {
				logger.debug("ZKRegistry CONNECTED, add listeners");
			}else if(StateListener.DISCONNECTED == state) {
				logger.debug("ZKRegistry DISCONNECTED");
			}else if(StateListener.RECONNECTED == state) {
				logger.debug("ZKRegistry Reconnected and reregist Services");
				for(Set<ServiceItem> sis : serviceItems.values()) {
					for(ServiceItem si: sis) {
						regist(si);
					}	
				}
			}
		});
		
		this.client.addChildListener(ServiceItem.ROOT, (path,children)->{
			serviceAdd(path,children);
		});
		List<String> childrens = this.client.getChildren(ServiceItem.ROOT);
		logger.debug("Service: "+childrens.toString());
		serviceAdd(ServiceItem.ROOT,childrens);
	}

	private void serviceAdd(String path, List<String> children) {
		
		for(String child : children){
			String data = this.client.data(path+"/"+child);
			String serviceInterName = ServiceItem.serviceName(child);
			logger.debug("service add: " + child);
			if(!serviceItems.containsKey(serviceInterName)){
				serviceItems.put(serviceInterName, new HashSet<ServiceItem>());
			}
			Set<ServiceItem> items = serviceItems.get(serviceInterName);
			items.add(new ServiceItem(data));
		}
		
	}

	@Override
	public void regist(ServiceItem item) {
		String key = item.key();
		logger.debug("regist service: "+key);
		this.client.create(key,item.val(), true);
	}

	@Override
	public void unregist(ServiceItem item) {
		String key = item.key();
		logger.debug("unregist service: "+key);
		this.client.delete(item.key());
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName, String method, Object[] args) {
		if(args != null && args.length > 0){
			int i = 0;
			Class<?>[] clazzes = new Class<?>[args.length];
			for(Object a : args){
				clazzes[i++] = a.getClass();
			}
			return this.getServices(serviceName, method, clazzes);
		}
		return Collections.EMPTY_SET;
	}

	@Override
	public Set<ServiceItem> getServices(String serviceName,String method,Class<?>[] args) {
		Set<ServiceItem> sis = this.serviceItems.get(serviceName);
		if(sis == null || sis.isEmpty()) {
			return Collections.EMPTY_SET;
		}
		Set<ServiceItem> fusings = new HashSet<ServiceItem>();
		Set<ServiceItem> set = new HashSet<ServiceItem>();
		
		for(ServiceItem si : sis) {
			if(si.isFusing()){
				fusings.add(si);
				continue;
			}
			for(ServiceMethod sm : si.getMethods()){
				if(sm.getMethodName().equals(method) 
						&& ServiceMethod.methodParamsKey(args).equals(sm.getMethodParamTypes())){
					if(sm.isFusing()){
						fusings.add(si);
					}else {
						set.add(si);
						break;
					}
				}
			}
		}
		if(set.isEmpty() && !fusings.isEmpty()){
			throw new  FusingException("Request services is fusing",fusings);
		}else {
			return set;
		}
		
	}

	@Override
	public boolean isExist(String serviceName) {
		for(String key : serviceItems.keySet()) {
			if(key.startsWith(serviceName)) {
				return true;
			}
		}
		return false;
	}
	
}
