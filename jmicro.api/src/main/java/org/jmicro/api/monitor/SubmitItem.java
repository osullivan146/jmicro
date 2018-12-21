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
package org.jmicro.api.monitor;

import java.util.Arrays;

import org.jmicro.api.net.IReq;
import org.jmicro.api.net.IResp;
import org.jmicro.api.net.Message;
import org.jmicro.api.registry.ServiceMethod;

/**
 * @author Yulei Ye
 * @date 2018年10月5日-下午12:50:47
 */
public final class SubmitItem{

	private byte level = MonitorConstant.LOG_DEBUG;
	
	private int type = -1;
	
	private Long linkId = null;
	
	private transient Throwable ex = null;
	
	private String exp = null;
	private String side = null;
	private String tagCls = null;
	private String instanceName = null;
	
	private String localHost = null;
	private String localPort = null;
	private String remoteHost = null;
	private String remotePort = null;
	
	private String serviceName = null;
	private String namespace = null;
	private String version = null;
	
	private String method = null;
	private Object[] reqArgs = null;
	
	private String[] others = new String[0];
	
	private Message msg  = null;
	
	private IReq req = null;
	
	private IResp resp = null;
	
	private ServiceMethod sm;
	
	//private Date date = new Date();
	
	private long time = 0;
	
	public void reset() {
		level = MonitorConstant.LOG_DEBUG;
		type = -1;
		linkId = null;
		
		exp = null;
		side = null;
		tagCls = null;
		localHost = null;
		localPort = null;
		remoteHost = null;
		remotePort = null;
		instanceName = null;
		
		serviceName = null;
		namespace = null;
		version = null;
		
		method = null;
		reqArgs = null;
		others = new String[0];
		msg  = null;
		req = null;
		resp = null;
		
		ex = null;
		
		sm = null;
		
	}
	
	public SubmitItem() {
		this.time = System.currentTimeMillis();
	}
	
	public SubmitItem(int type,byte level,long linkId,ServiceMethod sm,String[] others) {
		this(type,level,linkId,others);
		this.sm = sm;
	}
	
	public SubmitItem(int type,byte level,long linkId,String... others) {
		this(type);
		this.level = level;
		this.linkId = linkId;
		if(others.length > 0) {
			this.others = others;
		}
	}
	
	public SubmitItem(int type,byte level,Message msg,String... others) {
		this(type,level,msg.getLinkId(),others);
		this.msg = msg;
	}
	
	public SubmitItem(int type,byte level,long linkId,IReq req,String... others) {
		this(type,level,linkId,others);
		this.req = req;
	}
	
	public SubmitItem(int type,byte level,long linkId,IResp resp,String... others) {
		this(type,level,linkId,others);
		this.resp = resp;
	}
	
	public SubmitItem(int type) {
		this();
		this.type = type;
	}
	
	public SubmitItem(int type,Message msg) {
		this(type);
		this.msg = msg;
	}
	
	public SubmitItem(int type,IReq req) {
		this(type);
		this.req = req;
	}
	
	public SubmitItem(int type,IResp resp) {
		this(type);
		this.resp = resp;
	}
	
	public SubmitItem(int type,IReq req,IResp resp) {
		this(type);
		this.resp = resp;
		this.req = req;
	}
	
	public SubmitItem(int type,Message msg,IReq req,IResp resp) {
		this(type);
		this.resp = resp;
		this.req = req;
		this.msg = msg;
	}
	
	public String getLocalPort() {
		return localPort;
	}

	public void setLocalPort(String port) {
		this.localPort = port;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public String getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(String remotePort) {
		this.remotePort = remotePort;
	}

	public void setLinkId(Long linkId) {
		this.linkId = linkId;
	}

	public Throwable getEx() {
		return ex;
	}

	public void setEx(Throwable ex) {
		this.ex = ex;
	}

	public String getExp() {
		return exp;
	}

	public void setExp(String exp) {
		this.exp = exp;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}

	public void appendOther(String msg) {
		String[] arr = new String[this.others.length+1];
		System.arraycopy(this.others, 0, arr, 0, this.others.length);
		arr[arr.length-1] = msg;
		this.others = arr;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(byte level) {
		this.level = level;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getReqArgs() {
		return reqArgs;
	}

	public void setReqArgs(Object[] reqArgs) {
		this.reqArgs = reqArgs;
	}

	public String[] getOthers() {
		return others;
	}

	public void setOthers(String[] others) {
		this.others = others;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMsg(Message msg) {
		this.msg = msg;
	}

	public IReq getReq() {
		return req;
	}

	public void setReq(IReq req) {
		this.req = req;
	}

	public IResp getResp() {
		return resp;
	}

	public void setResp(IResp resp) {
		this.resp = resp;
	}

	public String getTagCls() {
		return tagCls;
	}

	public void setTagCls(String tagCls) {
		this.tagCls = tagCls;
	}

	public String getLocalHost() {
		return localHost;
	}

	public void setLocalHost(String host) {
		this.localHost = host;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public ServiceMethod getSm() {
		return sm;
	}

	public void setSm(ServiceMethod sm) {
		this.sm = sm;
	}

	@Override
	public String toString() {
		return "SubmitItem [level=" + level + ", type=" + Integer.toHexString(type).toUpperCase() + ", linkId=" + linkId + ", exp=" + exp + ", side="
				+ side + ", tagCls=" + tagCls + ", instanceName=" + instanceName + ", localHost=" + localHost
				+ ", localPort=" + localPort + ", remoteHost=" + remoteHost + ", remotePort=" + remotePort
				+ ", serviceName=" + serviceName + ", namespace=" + namespace + ", version=" + version + ", method="
				+ method + ", reqArgs=" + Arrays.toString(reqArgs) + ", others=" + Arrays.toString(others) + ", msg="
				+ msg + ", req=" + req + ", resp=" + resp + ", sm=" + sm + ", time=" + time + "]";
	}
	
}
