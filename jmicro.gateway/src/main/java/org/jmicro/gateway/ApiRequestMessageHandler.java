package org.jmicro.gateway;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jmicro.api.JMicro;
import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.client.AbstractClientServiceProxy;
import org.jmicro.api.client.IMessageCallback;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.gateway.ApiRequest;
import org.jmicro.api.gateway.ApiResponse;
import org.jmicro.api.idgenerator.IIdGenerator;
import org.jmicro.api.net.IMessageHandler;
import org.jmicro.api.net.ISession;
import org.jmicro.api.net.Message;
import org.jmicro.api.net.ServerError;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.registry.ServiceItem;
import org.jmicro.api.registry.ServiceMethod;
import org.jmicro.common.CommonException;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(side = Constants.SIDE_PROVIDER)
public class ApiRequestMessageHandler implements IMessageHandler{

	private final static Logger logger = LoggerFactory.getLogger(ApiRequestMessageHandler.class);
	
	@Inject
	private IIdGenerator idGenerator;
	
	@Inject
	private ICodecFactory codecFactory;
	
	@Inject
	private IObjectFactory objFactory;
	
	@Override
	public Short type() {
		return Constants.MSG_TYPE_API_REQ;
	}

	@Override
	public void onMessage(ISession session, Message msg) {
		ApiRequest req = ICodecFactory.decode(codecFactory, msg.getPayload(), 
				ApiRequest.class, msg.getProtocol());
		
		ApiResponse resp = new ApiResponse();
		Object result = null;
		Object srv = JMicro.getObjectFactory().getServie(req.getServiceName(), 
				req.getNamespace(), req.getVersion());
		
		msg.setType((short)(msg.getType()+1));
		resp.setReqId(req.getReqId());
		resp.setMsg(msg);
		
		if(srv != null){
			Class<?>[] clazzes = null;
			if(req.getArgs() != null && req.getArgs().length > 0){
				clazzes = new Class<?>[req.getArgs().length];
				for(int index = 0; index < req.getArgs().length; index++){
					clazzes[index] = req.getArgs()[index].getClass();
				}
			} else {
				clazzes = new Class<?>[0];
			}
			
			try {
				AbstractClientServiceProxy proxy = (AbstractClientServiceProxy)srv;
				ServiceItem si = proxy.getItem();
				if(si == null) {
					throw new CommonException("Service["+req.getServiceName()+"] namespace ["+req.getNamespace()+"] not found");
				}
				ServiceMethod sm = si.getMethod(req.getMethod(), clazzes);
				if(sm == null) {
					throw new CommonException("Service mehtod ["+req.getServiceName()+"] method ["+req.getMethod()+"] not found");
				}
				
				Method m = srv.getClass().getMethod(req.getMethod(), clazzes);
				
				if(!sm.needResponse) {
					result = m.invoke(srv, req.getArgs());
					return;
				}
				
				if(sm.stream) {
					IMessageCallback<Object> msgReceiver = (rst)->{
						if(session.isClose()) {
							return false;
						}
						resp.setSuccess(true);
						resp.setResult(rst);
						resp.setId(idGenerator.getLongId(ApiResponse.class));
						msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
						session.write(msg);
						return true;
					};
					JMicroContext.get().setParam(Constants.CONTEXT_CALLBACK_CLIENT, msgReceiver);
					result = m.invoke(srv, req.getArgs());
				} else {
					result = m.invoke(srv, req.getArgs());
					resp.setSuccess(true);
					resp.setResult(result);
					resp.setId(idGenerator.getLongId(ApiResponse.class));
					msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
					session.write(msg);
				}
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException 
					| IllegalArgumentException | InvocationTargetException | CommonException e) {
				logger.error("",e);
				result = new ServerError(0,e.getMessage());
				resp.setSuccess(false);
			}
		} else {
			resp.setSuccess(false);
			resp.setResult(result);
			resp.setId(idGenerator.getLongId(ApiResponse.class));
			msg.setPayload(ICodecFactory.encode(codecFactory, resp, msg.getProtocol()));
			session.write(msg);
		}
	}

}