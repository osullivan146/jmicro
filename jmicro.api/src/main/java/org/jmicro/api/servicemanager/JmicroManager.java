package org.jmicro.api.servicemanager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.annotation.JMethod;
import org.jmicro.api.annotation.Lazy;
import org.jmicro.api.exception.CommonException;
import org.jmicro.api.objectfactory.ProxyObject;
import org.jmicro.api.server.IInterceptor;
import org.jmicro.api.server.IRequest;
import org.jmicro.api.server.IRequestHandler;
import org.jmicro.api.server.IResponse;
import org.jmicro.api.server.IServerSession;
import org.jmicro.api.server.Message;
import org.jmicro.api.server.RpcRequest;
import org.jmicro.api.server.RpcResponse;
import org.jmicro.api.server.ServerError;
import org.jmicro.common.Constants;

@Component
@Lazy(true)
public class JmicroManager {

	/*private static JmicroManager ins = new JmicroManager();
	private JmicroManager(){}
	public static JmicroManager getIns() {return ins;}*/
	
	/*private IIdGenerator idGenerator = ComponentManager.getCommponentManager(IIdGenerator.class)
			.getComponent(Constants.DEFAULT_IDGENERATOR);*/
	
	private Queue<RpcRequest> requests = new ConcurrentLinkedQueue<RpcRequest>();
	private Object reqLock = new Object();
	
	private Queue<IResponse> responses = new ConcurrentLinkedQueue<IResponse>();
	private Object respLock = new Object();
	
	private Map<Long,IRequest> requestCache = new ConcurrentHashMap<>();
			
	@JMethod("init")
	public void init() {
		this.startReqWorker();
	}
	
	private Runnable reqHandler = () -> {
		for(;;) {
			IResponse resp = null;
			RpcRequest req = null;
			try {
				if(requests.isEmpty()) {
					synchronized(reqLock) {
						try {
							reqLock.wait();
						} catch (InterruptedException e) {
						}
					}
				}
				req = requests.poll();
				if(req == null){
					continue;
				}
				
				resp = handler(req);
			} catch (Throwable e) {
				if(req != null){
					resp = new RpcResponse(req.getRequestId(),new ServerError(0,e.getMessage()));
				}
			}
			if(resp != null) {
				Message msg = new Message();
				msg.setType(Message.PROTOCOL_TYPE_END);
				msg.setId(req.getMsg().getId());
				msg.setReqId(req.getRequestId());
				msg.setPayload(resp.encode());
				msg.setExt((byte)0);
				msg.setReq(false);
				msg.setSessionId(req.getSession().getSessionId());
				msg.setVersion(req.getMsg().getVersion());
				((IServerSession)req.getSession()).write(msg);
			}
		}
	};
	
	private Runnable respHandler = () -> {
		for(;;) {
			if(responses.isEmpty()) {
				synchronized(respLock) {
					try {
						respLock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			IResponse resp = responses.poll();
			IRequest req = requestCache.get(resp.getRequestId());
			//req.getSession().write(resp);
			requestCache.remove(resp.getRequestId());
			reqLock.notify();
		}
	};
	
	
	public void startReqWorker() {
		new Thread(reqHandler).start();
	}
	
	public void startRespWorker() {
		new Thread(respHandler).start();
	}
	
	public void addRequest(RpcRequest req) {
		synchronized(reqLock) {
			requests.offer(req);
			reqLock.notify();
		}
	}
	
	private void addResonse(IResponse resp) {
		synchronized(respLock) {
			responses.offer(resp);
			respLock.notify();
		}
		
	}
	
	private IResponse handler(RpcRequest req) {
		
		IRequestHandler handler = ComponentManager.getCommponentManager(IRequestHandler.class)
				.getComponent(Constants.DEFAULT_HANDLER);
		if(handler == null) {
			throw new CommonException("Handler ["+Constants.DEFAULT_HANDLER+"]");
		}
		
		IRequestHandler firstHandler = buildHanderChain(handler);
		if(firstHandler == null) {
			throw new CommonException("Handler is not found");
		}
		return firstHandler.onRequest(req);
	}
	
	private IRequestHandler buildHanderChain(IRequestHandler handler) {

		IInterceptor[] handlers = null;
		IInterceptor firstHandler = null;
		IInterceptor lastHandler = null;
		
		Collection<IInterceptor> hs = ComponentManager.getCommponentManager(IInterceptor.class)
				.getComponents();
		if(hs == null || hs.size() < 2) {
			throw new CommonException("IInterceptor is NULL");
		}
		
		int index = 1;
		handlers = new IInterceptor[hs.size()];
		
		for(Iterator<IInterceptor> ite = hs.iterator();ite.hasNext();){
			IInterceptor h = ite.next();
			Class<?> cls = ProxyObject.getTargetCls(h.getClass());
			if(cls.isAnnotationPresent(Interceptor.class)) {
				Interceptor ha = cls.getAnnotation(Interceptor.class);
				if(Constants.FIRST_INTERCEPTOR.equals(ha.value())){
					if(firstHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one ").append(Constants.FIRST_INTERCEPTOR).append(" found");
						sb.append(firstHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					firstHandler = h;
				}else if(Constants.LAST_INTERCEPTOR.equals(ha.value())){
					if(lastHandler != null){
						StringBuffer sb = new StringBuffer();
						sb.append("More than one [").append(Constants.LAST_INTERCEPTOR).append("] found");
						sb.append(lastHandler.getClass().getName()).append(", ").append(ha.getClass().getName());
						throw new CommonException(sb.toString());
					}
					lastHandler = h;
				} else {
					handlers[index] = h;
				}
			}else {
				handlers[index] = h;
			}
		}
		if(firstHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(Constants.FIRST_INTERCEPTOR)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[0] = firstHandler;
		
		if(lastHandler == null){
			StringBuffer sb = new StringBuffer("Interceptor not found [")
					.append(Constants.LAST_INTERCEPTOR)
					.append("]");
			throw new CommonException(sb.toString());
		}
		handlers[handlers.length-1] = lastHandler;
		
		IRequestHandler last = handler;
		for(int i = handlers.length-1; i >= 0; i--) {
			IInterceptor in = handlers[i];
			IRequestHandler next = last;
			last = new IRequestHandler(){
				@Override
				public IResponse onRequest(IRequest request) {
					return in.intercept(next, request);
				}
			};
		}

		return last;
	}
	
		
}