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
package cn.jmicro.choreography.agent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jmicro.api.IListener;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.choreography.AgentInfo;
import cn.jmicro.api.choreography.ChoyConstants;
import cn.jmicro.api.raft.IChildrenListener;
import cn.jmicro.api.raft.IDataListener;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.api.raft.INodeListener;
import cn.jmicro.choreography.controller.IAgentListener;
import cn.jmicro.choreography.instance.InstanceManager;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2019年1月23日 下午10:40:52
 */
@Component(level=20)
public class AgentManager {
	
	private final static Logger logger = LoggerFactory.getLogger(AgentManager.class);
	
	private final static String AGENT_IS_DESABLE = "AgentManager is disable by [\"/AgentManager/enable\"]";
	
	@Inject
	private IDataOperator op;
	
	@Inject
	private InstanceManager insManager;
	
	//路径到Agent映射
	private Map<String,AgentInfo> id2Agents = new ConcurrentHashMap<>();
	
	//分配的部署
	//private Map<String,Set<String>> id2Deps = new ConcurrentHashMap<>();
	
	private Set<IAgentListener> agentListeners = new HashSet<>();
	
	//Agent结点数据监听器
	private IDataListener agentDataListener = new IDataListener(){
		@Override
		public void dataChanged(String path, String data) {
			String id = path.substring(ChoyConstants.ROOT_AGENT.length()+1);
			updateAgentData(id,data);
		}
	};
	
	private final INodeListener activeNodeListener = (type, path, data)->{
		
		String id = path.substring(path.lastIndexOf("/")+1);
		String agentPath = ChoyConstants.ROOT_AGENT + "/" + id;
		
		if(IListener.REMOVE == type) {
			AgentInfo ai = id2Agents.remove(id);
			if(ai != null) {
				notifyAgentListener(IAgentListener.REMOVE,ai.getId(),ai);
				if(!insManager.isExistByAgentId(id)) {
					deleteAgent(agentPath);
				}
			}
		}else if(IListener.ADD == type) {
			String data1 = op.getData(agentPath);
			AgentInfo ai = JsonUtils.getIns().fromJson(data1, AgentInfo.class);
			if(!id2Agents.containsKey(id)) {
				id2Agents.put(id, ai);
				op.addDataListener(path, agentDataListener);
				notifyAgentListener(IAgentListener.ADD,id,ai);
			} else {
				id2Agents.put(id, ai);
			}
		}
	};
	
	public void ready() {
		
		op.addListener((state)->{
			if(Constants.CONN_CONNECTED == state) {
				logger.info("CONNECTED, reflesh children");
				//refleshAgent();
			}else if(Constants.CONN_LOST == state) {
				logger.warn("DISCONNECTED");
			}else if(Constants.CONN_RECONNECTED == state) {
				logger.warn("Reconnected,reflesh children");
				//refleshAgent();
			}
		});
		
		deleteInvalidAgent();
		
		logger.info("add listener");
		op.addChildrenListener(ChoyConstants.ROOT_AGENT, new IChildrenListener() {
			@Override
			public void childrenChanged(int type,String parent, String child,String data) {
				String path = ChoyConstants.ROOT_AGENT+"/"+child;
				String activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+child;
				
				op.addNodeListener(activePath, activeNodeListener);
				
				if(type == IListener.ADD) {
					if(op.exist(activePath)) {
						AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
						if(!id2Agents.containsKey(child)) {
							id2Agents.put(child, ai);
							op.addDataListener(path, agentDataListener);
							notifyAgentListener(IAgentListener.ADD,child,ai);
						} else {
							id2Agents.put(child, ai);
						}
					}
				}else if(type == IListener.REMOVE) {
					op.removeDataListener(path, agentDataListener);
					op.removeNodeListener(activePath, activeNodeListener);
					AgentInfo ai = id2Agents.remove(child);
					notifyAgentListener(IAgentListener.REMOVE,child,ai);
				}
			}
		});
	}
	
	private void deleteInvalidAgent() {
		Set<String> children = op.getChildren(ChoyConstants.ROOT_AGENT, false);
		if(children == null || children.isEmpty()) {
			return;
		}
		
		for(String c : children) {
			if(!insManager.isExistByAgentId(c)) {
				String activePath = ChoyConstants.ROOT_ACTIVE_AGENT + "/"+c;
				if(!op.exist(activePath)) {
					String path = ChoyConstants.ROOT_AGENT+"/"+c;
					deleteAgent(path);
				}
			}
		}
	}
	
	public boolean isActive(String agentId) {
		return this.id2Agents.containsKey(agentId);
	}
	
	private void deleteAgent(String path) {
		//String asDepPath = path + "/assign";
		Set<String>  asDeps = this.op.getChildren(path,false);
		if(asDeps != null && !asDeps.isEmpty()) {
			for(String c : asDeps) {
				op.deleteNode(path + "/" +c);
			}
		}
		op.deleteNode(path);
	}

	public Set<AgentInfo> getAllAgentInfo() {
		//refleshAgent();
		Set<AgentInfo> agents = new HashSet<>();
		agents.addAll(this.id2Agents.values());
		return agents;
	}
	
	public void addAgentListener(IAgentListener l) {

		if(this.agentListeners.contains(l) ) {
			return;
		}
		this.agentListeners.add(l);
		if(!id2Agents.isEmpty()) {
			for(AgentInfo ai : id2Agents.values()) {
				l.agentChanged(IAgentListener.ADD,ai.getId(), ai);
			}
		}
	}
	
	private void updateAgentData(String c, String data) {
		AgentInfo ai = JsonUtils.getIns().fromJson(data, AgentInfo.class);
		id2Agents.put(c, ai);
		notifyAgentListener(IAgentListener.DATA_CHANGE,ai.getId(),ai);
	}

	private void notifyAgentListener(int type,String agentId,AgentInfo ai) {
		if(this.agentListeners.isEmpty()) {
			return;
		}
		
		for(IAgentListener l : this.agentListeners) {
			l.agentChanged(type,agentId, ai);
		}
		
	}
	
}
