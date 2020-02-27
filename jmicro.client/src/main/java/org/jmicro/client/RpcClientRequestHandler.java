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
package org.jmicro.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.client.IClientSession;
import org.jmicro.api.client.IClientSessionManager;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.config.Config;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.exception.TimeoutException;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.loadbalance.ISelector;
import org.jmicro.api.monitor.MonitorConstant;
import org.jmicro.api.monitor.SF;
import org.jmicro.api.net.AbstractHandler;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.RpcResponse;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.pubsub.PSData;
import org.jmicro.api.pubsub.PubSubManager;
import org.jmicro.api.registry.AsyncConfig;
import org.jmicro.api.registry.Server;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.jmicro.common.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:07:12
 */
@Component(value=Constants.DEFAULT_CLIENT_HANDLER,side=Constants.SIDE_COMSUMER)
public class RpcClientRequestHandler extends AbstractHandler implements IRequestHandler, IMessageHandler {
	
    private final static Logger logger = LoggerFactory.getLogger(RpcClientRequestHandler.class);

    private static final Class<?> TAG = RpcClientRequestHandler.class;
	
	private final static Map<Long,IResponseHandler> waitForResponse = new ConcurrentHashMap<>();
	
	@Cfg("/RpcClientRequestHandler/openDebug")
	private boolean openDebug=false;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Cfg("/respBufferSize")
	private int respBufferSize  = Constants.DEFAULT_RESP_BUFFER_SIZE;
	
	@Inject(required=true)
	private IClientSessionManager sessionManager;
	
	@Inject(required=true)
	private ISelector selector;
	
	@Inject
	private PubSubManager pubsubManager;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	//测试统计模式使用
	//@Cfg(value="/RpcClientRequestHandler/clientStatis",defGlobal=false)
	//private boolean clientStatis=false;
	
	//private ServiceCounter counter = null;
	
	public void init() {
		/*if(clientStatis) {
			counter = new ServiceCounter("RpcClientRequestHandler",
					AbstractMonitorDataSubscriber.YTPES,10,2,TimeUnit.SECONDS);
			TimerTicker.getDefault(2000L).addListener("RpcClientRequestHandler", (key,att)->{
				System.out.println("======================================================");
				logger.debug("总请求:{}, 总响应:{},QPS:{}/S",
						counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BEGIN)
						,counter.getTotalWithEx(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR,MonitorConstant.CLIENT_REQ_OK,MonitorConstant.CLIENT_REQ_EXCEPTION_ERR)
						,counter.getAvg(TimeUnit.SECONDS,MonitorConstant.CLIENT_REQ_OK)
						);
			}, null);
		}*/
	}
	
	@Override
	public IResponse onRequest(IRequest request) {
		AbstractClientServiceProxy proxy =  (AbstractClientServiceProxy)JMicroContext.get().getObject(Constants.PROXY, null);
		RpcResponse resp = null;
		try {
			 ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
			 String mkey = sm.getKey().getMethod();
	         AsyncConfig ac = proxy.getAcs(mkey);
	         if(ac != null && ac.isEnable()) {
	        	 if(ac.getCondition().equals(AsyncConfig.ASYNC_DIRECT)) {
	        		 //客户端直接做异步
	        		 return doAsyncInvoke(proxy,request,sm,ac);
	        	 } else {
	        		 //可以异步调用并异步返回结果
		        	 request.putObject(mkey, ac);
	        	 }
	         }
			resp = doRequest(request,proxy);
		} catch (SecurityException | IllegalArgumentException  e) {
			throw new RpcException(request,"",e);
		}
		return resp;
	}

	private IResponse doAsyncInvoke(AbstractClientServiceProxy proxy, IRequest req,ServiceMethod sm,AsyncConfig ac) {
		
		String topic = sm.getKey().toKey(false, false, false);
		
		Map<String,Object> cxt = new HashMap<>();
		//结果回调RPC方法
		cxt.put(topic, ac);
		
		//链路相关ID
		cxt.put(JMicroContext.LINKER_ID, JMicroContext.lid());
		cxt.put(JMicroContext.REQ_ID, req.getRequestId());
		cxt.put(JMicroContext.MSG_ID, req.getMsgId());
		
		PSData data = new PSData();
		data.setContext(cxt);
		data.setData(req.getArgs());
		data.setFlag(PSData.flag(PSData.FLAG_QUEUE,PSData.FLAG_ASYNC_METHOD));
		data.setTopic(topic);
		
		//异步后,就不一定是本实例接收到此RPC调用了
		long id = pubsubManager.publish(data);
		
		RpcResponse resp = new RpcResponse();
		//resp.setResult(id);
		if(id < 0) {
			String msg = "ErrorCode:"+id+",异步调用失败"+sm.getKey().toKey(false, false, false);
			SF.doRequestLog(MonitorConstant.LOG_ERROR,TAG,req,null,msg);
			/*ServerError se = new ServerError();
			se.setErrorCode(ServerError.SE_ASYNC_PUBSUB_FAIL);
			se.setMsg(msg);
			resp.setResult(se);
			resp.setSuccess(false);
			*/
			throw new RpcException(req,msg);
		}
		
		return resp;
	}

	private RpcResponse doRequest(IRequest req, AbstractClientServiceProxy proxy) {
        
        ServerError se = null;
        		
        ServiceItem si = JMicroContext.get().getParam(Constants.DIRECT_SERVICE_ITEM,null);
        ServiceMethod sm = JMicroContext.get().getParam(Constants.SERVICE_METHOD_KEY, null);
        
        int retryCnt = -1;
        long interval = -1;
        long timeout = -1;
        //第一次进来在同一个线程中,同一个调用的超时重试使用
        boolean isFistLoop = true;
        
        long lid = 0;
        
        Message msg = new Message();
		msg.setType(Constants.MSG_TYPE_REQ_JRPC);
		msg.setProtocol(Message.PROTOCOL_BIN);
		msg.setReqId(req.getRequestId());
		
		msg.setVersion(Message.MSG_VERSION);
		msg.setLevel(Message.PRIORITY_NORMAL);
		
		boolean isDebug = false;
		
        do {
        	
        	if(si == null) {
        		//此方法可能抛出FusingException
        		si = selector.getService(req.getServiceName(),req.getMethod(),req.getArgs(),req.getNamespace(),
            			req.getVersion(), Constants.TRANSPORT_NETTY);
        	}
        	
        	if(si == null) {
        		SF.doSubmit(MonitorConstant.CLIENT_REQ_SERVICE_NOT_FOUND, req,null);
    			throw new CommonException("Service [" + req.getServiceName() + "] not found!");
    		}
        	
        	Server s = si.getServer(Constants.TRANSPORT_NETTY);
        	
        	JMicroContext.get().setParam(JMicroContext.REMOTE_HOST, s.getHost());
    		JMicroContext.get().setParam(JMicroContext.REMOTE_PORT, s.getPort()+"");
    		
    		//保存返回结果
    		final Map<String,Object> result = new HashMap<>();
    		
        	if(isFistLoop){
        		//超时重试时,只需要执行一次此代码块
        		isFistLoop = false;
        		retryCnt = sm.getRetryCnt();
        		if(retryCnt < 0){
        			retryCnt = si.getRetryCnt();
        		}
        		
        		interval = sm.getRetryInterval();
    			if(interval < 0){
    				interval = si.getRetryInterval();
    			}
    			interval = TimeUtils.getMilliseconds(interval, sm.getBaseTimeUnit());
    			
    			timeout = sm.getTimeout();
				if(timeout < 0){
					timeout = si.getTimeout();
				}
				timeout = TimeUtils.getMilliseconds(timeout, sm.getBaseTimeUnit());
				
				//msg.setStream(sm.isStream());
				//是否记录二进制流数据到日志文件
				msg.setDumpDownStream(sm.isDumpDownStream());
				msg.setDumpUpStream(sm.isDumpUpStream());
	    		msg.setNeedResponse(sm.isNeedResponse());
	    		msg.setLoggable(JMicroContext.get().isLoggable(false));
	    		
	    		int f = sm.getMonitorEnable() == 1 ? 1 : (si.getMonitorEnable()== 1?1:0);
	    		msg.setMonitorable(f == 1);
	    		
	    		f = sm.getDebugMode() == 1 ? 1 : (si.getDebugMode()== 1?1:0);
	    		isDebug = f == 1;
	    		msg.setDebugMode(isDebug);
	    		
    			msg.setLinkId(JMicroContext.lid());
    			
	    		if(isDebug) {
	    			//开启Debug模式，设置更多信息在消息包中，网络流及编码会有损耗，但更晚于问题追踪
	    			msg.setInstanceName(Config.getInstanceName());
	    			msg.setTime(System.currentTimeMillis());
	    			msg.setMethod(sm.getKey().getMethod());
	    		}
	    		
	    		//超时重试不需要重复注册监听器
    			waitForResponse.put(req.getRequestId(), (message)->{
    				result.put("msg", message);
    				//在请求响应之间做同步
    				synchronized(req) {
        				req.notify();
        			}
    			});
    			//LogUtil.A.debug("Put waitForResponse reqID:{},keySet:{}",req.getRequestId(),waitForResponse.keySet());
        	}
        	
        	Object pl = ICodecFactory.encode(this.codecFactory, req, msg.getProtocol());
        	msg.setPayload(pl);
        	
        	//JMicroContext.get().setParam(Constants.SERVICE_METHOD_KEY, sm);
    		//JMicroContext.get().setParam(Constants.SERVICE_ITEM_KEY, si);
    		//JMicroContext.setSrvLoggable();
    		//msg.setLoggable(JMicroContext.get().isLoggable(false));
    	    
    	    IClientSession session = this.sessionManager.getOrConnect(s.getHost(), s.getPort());
    		
    	    if(isDebug) {
    	    	msg.setId(this.idGenerator.getLongId(Message.class));
    	    }
    		
    		if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
    			SF.doRequestLog(MonitorConstant.LOG_DEBUG,TAG,req,null," do request");
    		}
    		
    		//long st = System.currentTimeMillis();
    		//logger.info(""+st);
    		session.write(msg);
    		
    		if(!sm.isNeedResponse()) {
    			//数据发送后，不需要返回结果，也不需要请求确认包，直接返回
    			//this.sessionManager.write(msg, null,retryCnt);
    			if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
    				SF.doServiceLog(MonitorConstant.LOG_DEBUG,TAG,sm,null, " no need response and return");
        		}
    			waitForResponse.remove(req.getRequestId());
    			return null;
    		}
    		
    		SF.doSubmit(MonitorConstant.CLIENT_REQ_BEGIN, req,null);
    		session.increment(MonitorConstant.CLIENT_REQ_BEGIN);
    		
    		synchronized(req) {
    			try {
    				//logger.info("waiting ReqID: {},timeout:{}",msg.getReqId(),timeout);
    				if(timeout > 0){
    					req.wait(timeout);
    				} else {
    					req.wait();
    				}
    			} catch (InterruptedException e) {
    				logger.error("timeout: ",e);
    			}
    		}
    		
    		//long et = System.currentTimeMillis();
    		//System.out.println(req.getRequestId() + " used: "+(et-st));
    		
    		
    		Message respMsg = (Message)result.get("msg");
    		result.clear();
    		RpcResponse resp = null;
    		if(respMsg != null){
    			if(respMsg.getPayload() != null){
    				resp = ICodecFactory.decode(this.codecFactory,respMsg.getPayload(),
    						RpcResponse.class,msg.getProtocol());
    				
    				resp.setMsg(respMsg);
    				//req.setMsg(msg);
    				if(SF.isLoggable(this.openDebug,MonitorConstant.LOG_DEBUG)) {
        				SF.doResponseLog(MonitorConstant.LOG_DEBUG,TAG,resp,null,"reqID ["+resp.getReqId()+"] response");
            		}
    			} else {
        			SF.doMessageLog(MonitorConstant.LOG_ERROR,TAG,respMsg,null,"reqID ["+resp.getReqId()+"] response");
        		}
    		} else {
    			SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT, req, null);
    			session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT);
    		}
    		
    		if(resp != null && resp.isSuccess() && !(resp.getResult() instanceof ServerError)) {
				session.increment(MonitorConstant.CLIENT_REQ_OK);
    			SF.doSubmit(MonitorConstant.CLIENT_REQ_OK, req, resp,null);
				//同步请求成功，直接返回
    			req.setFinish(true);
    			waitForResponse.remove(req.getRequestId());
    			//LogUtil.A.debug("Remove waitForResponse reqID:{}",req.getRequestId());
    			return resp;
    		}
    		
    		//下面是此次请求失败,进入重试处理过程
    		StringBuffer sb = new StringBuffer();
			if(se!= null){
				sb.append(se.toString());
			}
			sb.append(" host[").append(s.getHost()).append("] port [").append(s.getPort())
			.append("] service[").append(si.getKey().getServiceName())
			.append("] method[").append(sm.getKey().getMethod())
			.append("] param[").append(sm.getKey().getParamsStr())
			.append("] param value[").append(req.getArgs()!= null?argStr(req.getArgs()):"null");
    		
    		if(resp == null){
    			if(retryCnt > 0){
    				//还可以重试
    				sb.append("] do retry: ").append(retryCnt);
    				//SF.doRequestLog(MonitorConstant.WARN,msg.getLinkId(),TAG,req,null,sb.toString());
    				//logger.error("reqId:{}, waitForResponse keySet:{}",req.getRequestId(),waitForResponse.keySet());
    			} else {
    				//不能再重试了
    				//断开新打开连接
    				session.increment(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL);
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_TIMEOUT_FAIL, req, null);
    				
    				sb.append("] timeout[").append(timeout).append("] request and stop retry: ").append(retryCnt)
    				.append(",reqId:").append(req.getRequestId()).append(", LinkId:").append(lid);
    				SF.doRequestLog(MonitorConstant.LOG_ERROR,TAG,req,null,sb.toString());
    				
    				if(session.getFailPercent() > 50) {
        				logger.warn("session.getFailPercent() > 50,Close session: {},Percent:{},sessionID:{}",sb.toString(),session.getFailPercent(),session.getId());
        				this.sessionManager.closeSession(session);
    				}
    				logger.error(sb.toString()+",Hander:" + waitForResponse.get(req.getRequestId()));
    				//肯定是超时了
    				throw new TimeoutException(req,sb.toString());
    			}
    		
    			if(interval > 0 && retryCnt > 0){
    				try {
    					//超时重试间隔
    					Thread.sleep(si.getRetryInterval());
    				} catch (InterruptedException e) {
    					logger.error("Sleep exceptoin ",e);
    				}
    				
    				//logger.warn(sb.toString()+",reqId:"+req.getRequestId());
    			
    				SF.doRequestLog(MonitorConstant.LOG_WARN,TAG,req,null," do retry");
    				SF.doSubmit(MonitorConstant.CLIENT_REQ_RETRY, req, resp,null);
    				session.increment(MonitorConstant.CLIENT_REQ_RETRY);
    				continue;//重试循环
    			}
    			
    		}else if(resp.getResult() instanceof ServerError){
				//服务器已经发生错误,是否需要重试
				 se = (ServerError)resp.getResult();
				 //logger.error("error code: "+se.getErrorCode()+" ,msg: "+se.getMsg());
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR, req, null);
				 SF.doResponseLog(MonitorConstant.LOG_ERROR,TAG,resp,null,se.toString());
				 session.increment(MonitorConstant.CLIENT_REQ_EXCEPTION_ERR);
				 waitForResponse.remove(req.getRequestId());
				 throw new RpcException(req,sb.toString());
			} else if(!resp.isSuccess()){
				 //服务器正常逻辑处理错误，不需要重试，直接失败
				 req.setSuccess(resp.isSuccess());
				 SF.doSubmit(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR, req, resp,null);
				 SF.doResponseLog(MonitorConstant.LOG_ERROR,TAG,resp,null);
				 session.increment(MonitorConstant.CLIENT_REQ_BUSSINESS_ERR);
				 waitForResponse.remove(req.getRequestId());
			     throw new RpcException(req,sb.toString());
			}
    		//waitForResponse.remove(req.getRequestId());
    		//代码不应该走到这里，如果走到这里，说明系统还有问题
    		throw new CommonException(sb.toString());
    		
        }while(retryCnt-- > 0);
        throw new CommonException("Service:"+req.getServiceName()+", Method: "+req.getMethod()+", Params: "+req.getArgs());
	}

	private String argStr(Object[] args) {
		if(args == null || args.length == 0) {
			return "null";
		}
		
		StringBuffer sb = new StringBuffer();
		for(Object a : args) {
			if(a == null) {
				sb.append("null").append(",");
			}else {
				sb.append(a.toString()).append(",");
			}
		}
		sb = sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	@Override
	public Byte type() {
		return Constants.MSG_TYPE_RRESP_JRPC;
	}

	@Override
	public void onMessage(ISession session,Message msg) {
		//receive response
		IResponseHandler handler = waitForResponse.get(msg.getReqId());
		if(msg.isLoggable()) {
			SF.doMessageLog(MonitorConstant.LOG_DEBUG,TAG,msg,null," receive message");
		}
		if(handler!= null){
			//logger.info("get result reqID: {}",msg.getReqId());
			handler.onResponse(msg);
		} else {
			SF.doMessageLog(MonitorConstant.LOG_ERROR,TAG,msg,null," handler not found");
			logger.error("msdId:"+msg.getId()+",reqId:"+msg.getReqId()+",linkId:"+msg.getLinkId()+
					",waitForResponse keySet"+waitForResponse.keySet());
			session.increment(ISession.CLIENT_HANDLER_NOT_FOUND);
			if(session.getTakePercent(ISession.CLIENT_HANDLER_NOT_FOUND) > 50) {
				//断开重连
				this.sessionManager.closeSession(session);
			}
		}
	}
	
	private static interface IResponseHandler{
		void onResponse(Message msg);
	}
}

