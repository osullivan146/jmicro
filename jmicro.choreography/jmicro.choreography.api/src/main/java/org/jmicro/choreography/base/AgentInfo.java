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
package org.jmicro.choreography.base;

import java.util.HashSet;
import java.util.Set;

import org.jmicro.api.annotation.IDStrategy;

@IDStrategy(1)
public final class AgentInfo {

	//每个Agent每次运行分配唯一ID
	private String id;
	
	//JMicro instance name
	private String name;
	
	private String host;
	
	//启动此Agent JVM的命令，包括java及后面的全部参数
	private String cmd;
	
	//yyyy-MM-dd:hh:mm:ss
	private long startTime;
	
	private long assignTime;
	
	private float cprRate;
	
	private float memoryRate;
	
	//private HashSet<String> lockerDeps = new HashSet<>();
	
	private HashSet<String> runningDeps = new HashSet<>();
	
	private HashSet<String> assignDeps = new HashSet<>();
	
	private HashSet<String> deleteDeps = new HashSet<>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public float getCprRate() {
		return cprRate;
	}

	public void setCprRate(float cprRate) {
		this.cprRate = cprRate;
	}

	public float getMemoryRate() {
		return memoryRate;
	}

	public void setMemoryRate(float memoryRate) {
		this.memoryRate = memoryRate;
	}

	public Set<String> getRunningDeps() {
		return runningDeps;
	}

	public Set<String> getAssignDeps() {
		return assignDeps;
	}

	public Set<String> getDeleteDeps() {
		return deleteDeps;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getAssignTime() {
		return assignTime;
	}

	public void setAssignTime(long assignTime) {
		this.assignTime = assignTime;
	}

	@Override
	public String toString() {
		return "AgentInfo [id=" + id + ", name=" + name + ", cmd=" + cmd + ", startTime=" + startTime + "]";
	}

}