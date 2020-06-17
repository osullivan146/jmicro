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
package cn.jmicro.main.monitor.v2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cn.jmicro.api.JMicro;
import cn.jmicro.api.annotation.Cfg;
import cn.jmicro.api.annotation.Component;
import cn.jmicro.api.annotation.Inject;
import cn.jmicro.api.annotation.SMethod;
import cn.jmicro.api.annotation.Service;
import cn.jmicro.api.mng.ReportData;
import cn.jmicro.api.monitor.IMonitorDataSubscriber;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.api.monitor.MRpcItem;
import cn.jmicro.api.raft.IDataOperator;
import cn.jmicro.common.Utils;
import cn.jmicro.common.util.JsonUtils;
import cn.jmicro.monitor.api.AbstractMonitorDataSubscriber;

/**
 * 
 * 
 * @author Yulei Ye
 * @date 2020年1月18日
 */
@Component
@Service(version="0.0.1", namespace="log2DbMonitor",monitorEnable=0)
public class Log2DbMonitor extends AbstractMonitorDataSubscriber implements IMonitorDataSubscriber {

	private final static Logger logger = LoggerFactory.getLogger(Log2DbMonitor.class);
	
	@Cfg(value="/Monitor/Log2DbMonitor/enable",defGlobal=false)
	private boolean enable = true;
	
	@Cfg(value="/Log2DbMonitor/openDebug",defGlobal=false)
	private boolean openDebug=false;
	
	@Inject
	private MongoDatabase mongoDb;
	
	@Inject
	private IDataOperator op;
	
	private List<MRpcItem> siq = new LinkedList<>();
	
	public static void main(String[] args) {
		 JMicro.getObjectFactoryAndStart(args);
		 Utils.getIns().waitForShutdown();
	}
	
	public void ready() {
		String skey = this.skey("log2DbMonitor", "0.0.1");
		registType(op,skey, MC.MT_TYPES_ARR);
		new Thread(this::doLog).start();
	}
	
	private void doLog() {
		List<MRpcItem> temp = new LinkedList<>();
		while(true) {
			try {
				if(!siq.isEmpty()) {
					synchronized(siq) {
						temp.addAll(siq);
						siq.clear();
					}
					saveLog(temp);
				}
				try {
					Thread.sleep(2000);;
				} catch (Exception e) {
				}
			}catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private void saveLog(List<MRpcItem> temp) {
		if(this.openDebug) {
			logger.debug("printLog One LOOP");
		}
		
		if(temp == null || temp.isEmpty()) {
			return;
		}
		
		List<Document> docs = new ArrayList<>();
		synchronized(temp) {
			Iterator<MRpcItem> itesm = temp.iterator();
			for(;itesm.hasNext();) {
				Document d = toLog(itesm.next());
				if(d != null) {
					docs.add(d);
				}
				itesm.remove();
			}
		}
		
		MongoCollection<Document> coll = mongoDb.getCollection("linker_log");
		coll.insertMany(docs);
	}

	private Document toLog(MRpcItem si) {
		Document d = Document.parse(JsonUtils.getIns().toJson(si));
		return d;
	}
	
	@Override
	@SMethod(needResponse=false,asyncable=true)
	public void onSubmit(MRpcItem[] sis) {
		
		for(MRpcItem si : sis) {
			try {
			if(openDebug) {
				logger.debug("LinkRouterMonitor:{}",si);
			}
			
			/*if(si.getType() != MonitorConstant.LINKER_ROUTER_MONITOR) {
				logger.warn("LinkRouterMonitor LOG TYPE ERROR:{}",si);
				return;
			}*/
			
			synchronized(siq) {
				siq.add(si);
			}
			} catch (Throwable e) {
				logger.error("LinkRouterMonitor GOT ERROR:" + si.toString(),e);
			}
		}
		
	}
	
	
	public ReportData  getData(String srvKey,Short[] type, String[] dataType) {
		return null;
	}
}
