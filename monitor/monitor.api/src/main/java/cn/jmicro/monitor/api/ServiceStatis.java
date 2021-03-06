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
package cn.jmicro.monitor.api;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月22日-下午1:42:56
 */
public class ServiceStatis {

	private String service;
	private long time;
	//public long endtime;
	private int avgResponseTime;
	
	public ServiceStatis(){};
	
	public ServiceStatis(String service,long time,int avgResponseTime){
		this.service = service;
		this.time = time;
		this.avgResponseTime = avgResponseTime;
	};
	
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public int getAvgResponseTime() {
		return avgResponseTime;
	}
	public void setAvgResponseTime(int avgResponseTime) {
		this.avgResponseTime = avgResponseTime;
	}
	
	
}
