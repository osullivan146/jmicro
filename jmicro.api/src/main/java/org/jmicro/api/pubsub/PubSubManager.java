package org.jmicro.api.pubsub;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.annotation.Reference;
import org.jmicro.api.config.Config;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.api.registry.IRegistry;
import org.jmicro.api.registry.IServiceListener;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.api.registry.UniqueServiceMethodKey;
import org.jmicro.api.service.ServiceManager;
import org.jmicro.common.Constants;
import org.jmicro.common.util.JsonUtils;
import org.jmicro.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年12月22日 下午11:10:50
 */
@Component(value="pubSubManager")
public class PubSubManager {

	private final static Logger logger = LoggerFactory.getLogger(PubSubManager.class);
	
	@Inject
	private IRegistry registry;
	
	@Inject
	private IDataOperator dataOp;
	
	@Inject
	private ServiceManager srvManager;
	
	/**
	 * default pubsub server
	 */
	@Reference(namespace=Constants.DEFAULT_PUBSUB,version="0.0.1",required=false)
	private IInternalSubRpc defaultServer;
	
	/**
	 * is enable pubsub feature
	 */
	@Cfg(value="/PubSubManager/enable",defGlobal=false)
	private boolean enable = false;
	
	@Cfg(value="/PubSubManager/openDebug",defGlobal=false)
	private boolean openDebug = true;
	
	/**
	 * default pubsub server name
	 */
	//@Cfg(value="/PubSubManager/defaultServerName",changeListener="init")
	//private String defaultServerName = Constants.DEFAULT_PUBSUB;
	
	/**
	 * topic listener
	 */
	//private Set<ITopicListener> topicListeners = new HashSet<>();
	
	/**
	 * The directory of the structure
	 * PubSubDir is the root directory
	 * Topic is pubsub topic and sub node is the listener of service method
	 * 
	 *            |            |--L2
	 *            |----topic1--|--L1
	 *            |            |--L3 
	 *            |
	 *            |            |--L1 
	 *            |            |--L2
	 *            |----topic2--|--L3
	 *            |            |--L4
	 * PubSubDir--|            |--L5   
	 *            |
	 *            |            |--L1
	 *            |            |--L2
	 *            |            |--L3
	 *            |----topic3--|--L4
	 *            |            |--L5
	 *                         |--L6
	 *
	 */
	private Set<ISubsListener> subListeners = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * subscriber path to context list
	 * key is subscriber path and value the context
	 */
	//private Map<String,Map<String,String>> path2SrvContext = new HashMap<>();
	
	private Map<String,Set<String>> topic2Method = new ConcurrentHashMap<>();
	
	private Map<String,Boolean> srvs = new HashMap<>();
	
	/*private INodeListener topicNodeListener = new INodeListener(){
		public void nodeChanged(int type, String path,String data){
			if(type == INodeListener.NODE_ADD){
				logger.error("NodeListener service add "+type+",path: "+path);
			} else if(type == INodeListener.NODE_REMOVE) {
				logger.error("service remove:"+type+",path: "+path);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+path);
			}
		}
	};*/
	
	/**
	 * 监听全部服务的增加操作，判断是否有订阅方法，如果有，则注册到对应的主是下面
	 */
	private IServiceListener serviceParseListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, ServiceItem item) {
			if(type == IServiceListener.SERVICE_ADD) {
				parseServiceAdded(item);
			}else if(type == IServiceListener.SERVICE_REMOVE) {
				//serviceRemoved(item);
			}else if(type == IServiceListener.SERVICE_DATA_CHANGE) {
				//serviceDataChange(item);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+item.getKey().toKey(true, true, true));
			}
		}
	};
	
	private IServiceListener serviceAddedRemoveListener = new IServiceListener() {
		@Override
		public void serviceChanged(int type, ServiceItem item) {
			if(type == IServiceListener.SERVICE_ADD) {
				//parseServiceAdded(item);
			}else if(type == IServiceListener.SERVICE_REMOVE) {
				serviceRemoved(item);
			}else if(type == IServiceListener.SERVICE_DATA_CHANGE) {
				serviceDataChange(item);
			} else {
				logger.error("rev invalid Node event type : "+type+",path: "+item.getKey().toKey(true, true, true));
			}
		}
	};
	
	public void init1() {
		if(!enable) {
			//不启用pubsub Server功能，此运行实例是一个
			logger.info("Pubsub server is disable by config [/PubSubManager/enable]");
			return;
		}
		
		/*if(pubSubServers.isEmpty()) {
			throw new CommonException("No pubsub server found, pubsub is disable!");
		}
		
		if(!StringUtils.isEmpty(defaultServerName)) {
			IInternalSubRpc s = this.pubSubServers.get(defaultServerName);
			if(s == null) {
				logger.error("server [{}] not found",defaultServerName);
			}
			defaultServer = s;
		}*/
		
		/*logger.info("add listener");
		this.dataOp.addChildrenListener(Config.PubSubDir, new IChildrenListener() {
			@Override
			public void childrenChanged(String path, List<String> children) {
				topicsAdd(children);
			}
		});	
		*/
		Set<String> children = this.dataOp.getChildren(Config.PubSubDir);
		for(String t : children) {
			Set<String>  subs = this.dataOp.getChildren(Config.PubSubDir+"/"+t);
			for(String sub : subs) {
				this.dataOp.deleteNode(Config.PubSubDir+"/"+t+"/"+sub);
			}
		}
		
		srvManager.addListener(serviceParseListener);
	}
	
	protected void serviceDataChange(ServiceItem item) {
		
	}

	protected void serviceRemoved(ServiceItem item) {
		
		for(ServiceMethod sm : item.getMethods()) {
			if(StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			
			if(this.topic2Method.containsKey(sm.getTopic())) {
				String mk = sm.getKey().toKey(false, false, false).intern();
				Set<String> ms = this.topic2Method.get(sm.getTopic());
				ms.remove(mk);
				
				if(ms.isEmpty()) {
					this.topic2Method.remove(sm.getTopic());
				}
				
				this.unsubcribe(null,sm);
			}
			
			this.notifySubListener(ISubsListener.SUB_REMOVE, sm.getTopic(), sm.getKey(), null);
		}
		String key = item.serviceName();
		if(srvs.containsKey(key)) {
			srvs.remove(key);
		}
		registry.removeServiceListener(key, serviceAddedRemoveListener);
		
	}

	protected void parseServiceAdded(ServiceItem item) {
		if(item == null || item.getMethods() == null) {
			return;
		}
		
		boolean flag = false;
		
		for(ServiceMethod sm : item.getMethods()) {
			if(StringUtils.isEmpty(sm.getTopic())) {
				continue;
			}
			flag = true;
			
			if(!this.topic2Method.containsKey(sm.getTopic())) {
				this.topic2Method.put(sm.getTopic(), new HashSet<>());
			}
			
			String mk = sm.getKey().toKey(false, false, false).intern();
			Set<String> ms = this.topic2Method.get(sm.getTopic());
			if(!ms.contains(mk)) {
				this.subscribe(null, sm);
				ms.add(mk);
				if(openDebug) {
					logger.debug("Got ont CB: {}",mk);
				}
				this.notifySubListener(ISubsListener.SUB_ADD, sm.getTopic(), sm.getKey(), null);
			}
		}
		
		String key = item.serviceName();
		if(flag && !srvs.containsKey(key)) {
			srvs.put(key, true);
			registry.addExistsServiceListener(key, serviceAddedRemoveListener);
		}
	}

	/*public void addTopicListener(ITopicListener l) {
		topicListeners.add(l);
	}
	
	public void removeTopicListener(ITopicListener l) {
		topicListeners.remove(l);
	}
	
	public void notifyTopicListener(byte type,String topic,Map<String,String> context) {
		if(topicListeners.isEmpty()) {
			return;
		}
		
		Iterator<ITopicListener> ite = this.topicListeners.iterator();
		ITopicListener l = null;
		while(ite.hasNext()) {
			l = ite.next();
			l.on(type, topic, context);
		}
	}*/
	
	public void addSubsListener(ISubsListener l) {
		if(subListeners == null) {
			subListeners = new HashSet<ISubsListener>();
		}
		subListeners.add(l);
		
		if(!this.topic2Method.isEmpty()) {
			for(Map.Entry<String, Set<String>> e : topic2Method.entrySet()) {
				for(String key : e.getValue()) {
					UniqueServiceMethodKey k = UniqueServiceMethodKey.fromKey(key);
					l.on(ISubsListener.SUB_ADD, e.getKey(), k,null);
				}
			}
		}
	}
	
	public void removeSubsListener(ISubsListener l) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.contains(l)) {
			subs.remove(l);
		}
	}
	
	public boolean publish(Map<String,Object> context, String topic, String content) {

		IInternalSubRpc s = this.defaultServer;// this.getServer(context);
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		
		return this.publish(item);
		
	}
	
	public boolean publish(Map<String,Object> context,String topic, byte[] content) {
		
		PSData item = new PSData();
		item.setTopic(topic);
		item.setData(content);
		item.setContext(context);
		
		return this.publish(item);
	}

	public boolean publish(PSData item) {
		IInternalSubRpc s = this.defaultServer;//this.getServer(item.getContext());
		if(s == null) {
			logger.error("No Pubsub server for topic:{}",item.getTopic());
			return false;
		}
		if(openDebug) {
			logger.debug("Publish topic: {}, data: {}",item.getTopic(),item.getData());
		}
		return s.publishData(item);
	}

	private boolean subscribe(Map<String,String> context,ServiceMethod sm) {
		String p = this.getPath(sm);
		String cxt = context == null ? "":JsonUtils.getIns().toJson(context);
		if(!dataOp.exist(p)) {
			dataOp.createNode(p,cxt,true);
		}
		return true;
	}

	private boolean unsubcribe(Map<String,String> context,ServiceMethod sm) {
		String p = this.getPath(sm);
		dataOp.deleteNode(p);
		return true;
	}
	
	private String getPath(ServiceMethod sm) {
		String p = Config.PubSubDir+"/" + sm.getTopic().replaceAll("/", "_")+"/"+sm.getKey().toKey(false, false, false);
	    return p;
	}

	private void notifySubListener(byte type,String topic,UniqueServiceMethodKey k,Map<String,String> context) {
		Set<ISubsListener> subs = subListeners;
		if(subs != null && subs.isEmpty()) {
			return;
		}
		
		/*String topic = this.getTopic(path);
		UniqueServiceMethodKey k = this.getMethodKey(path); */
		
		Iterator<ISubsListener> ite = subs.iterator();
		ISubsListener l = null;
		while(ite.hasNext()) {
			l = ite.next();
			l.on(type, topic,k, context);
		}
		
	}

	public boolean isEnable() {
		return enable;
	}
	
}
