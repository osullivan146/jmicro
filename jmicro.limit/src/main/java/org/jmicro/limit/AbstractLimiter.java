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
package org.jmicro.limit;

import java.util.Set;

import org.jmicro.api.JMicro;
import org.jmicro.api.limitspeed.ILimiter;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.server.IRequest;
import org.jmicro.common.CommonException;
import org.jmicro.common.util.StringUtils;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月17日-下午5:44:10
 */
public abstract class AbstractLimiter implements ILimiter{

	protected String serviceKey(IRequest req){
		String key = req.getServiceName()+req.getMethod();
		if(req.getArgs() == null || req.getArgs().length == 0){
			return key;
		}
		for(Object o: req.getArgs()){
			key = key + o.getClass().getName();
		}
		return key;
	}
	

	protected ServiceItem getServiceItem(IRequest req) {
		Set<ServiceItem> sis = JMicro.getRegistry(null).getServices(req.getServiceName(),req.getMethod(),req.getArgs(),
				req.getNamespace(),req.getVersion());
		if(sis == null || sis.isEmpty()){
			return null;
		}
		return sis.iterator().next();
	}
	
	protected ServiceMethod getServiceMethod(ServiceItem item ,IRequest req){
		ServiceItem si = this.getServiceItem(req);
		if(si == null) {
			throw new CommonException("Service not found: "+ req.getServiceName());
		}
		ServiceMethod sm = null;
		for(ServiceMethod mi : si.getMethods()){
			if(mi.getMethodName().equals(req.getMethod())){
				sm = mi;
				break;
			}
		}
		
		if(sm == null) {
			throw new CommonException("Service method not found: "+ req.getServiceName()+"."+req.getMethod());
		}
		return sm;
	}
	
	protected String getSpeedUnit(IRequest req){
		ServiceItem si = this.getServiceItem(req);
		if(si == null) {
			throw new CommonException("Service not found: "+ req.getServiceName());
		}
		ServiceMethod sm = this.getServiceMethod(si, req);
		
		if(StringUtils.isEmpty(sm.getSpeedUnit())){
			return si.getSpeedUnit();
		}else {
			return sm.getSpeedUnit();
		}
	}
	
	protected int getSpeed(IRequest req) {
		ServiceItem si = this.getServiceItem(req);
		if(si == null) {
			throw new CommonException("Service not found: "+ req.getServiceName());
		}
		ServiceMethod sm = this.getServiceMethod(si, req);
		
		int maxSpeed = sm.getMaxSpeed();
		if(maxSpeed == 0){
			//not limit
			return 0;
		}
		
		if(maxSpeed < 0){
			//decide by Service
			maxSpeed = si.getMaxSpeed();
		}
		
		if(maxSpeed <= 0){
			//not limit
			return 0;
		}
		return maxSpeed;
	}
}