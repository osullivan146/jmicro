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
package cn.jmicro.api.route;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import cn.jmicro.api.registry.Server;
import cn.jmicro.api.registry.ServiceItem;
import cn.jmicro.common.util.StringUtils;

/**
 * 
 *
 * @author Yulei Ye
 * @date: 2018年11月11日 下午8:36:24
 */
public abstract class AbstractRouter implements IRouter{

	protected boolean filterByClient(Iterator<RouteRule> ite, String key, String val) {
		if(StringUtils.isEmpty(val)) {
			return false; 
		}
		String ctxVal = RouteUtils.getCtxParam(key);
		if(StringUtils.isEmpty(ctxVal) || StringUtils.isEmpty(key) || StringUtils.isEmpty(val)
				|| !ctxVal.equals(val) ) {
			ite.remove();
			return true;
		}
		return false;
	}
	
	protected HashSet<ServiceItem> filterServicesByTargetIpPort(RouteRule rule, Set<ServiceItem> services,String transport) {
	   
		String ipPort = rule.getTo().getIpPort();
		
		HashSet<ServiceItem> items = new HashSet<>();
		
		for(ServiceItem si : services) {
			Server s = si.getServer(transport);
			if(s == null) {
				continue;
			}
			String sipPort = s.getHost()+":"+s.getPort();
			if(sipPort.startsWith(ipPort)) {
				items.add(si);
			}
		}
		
		return items;
	}
	
}
