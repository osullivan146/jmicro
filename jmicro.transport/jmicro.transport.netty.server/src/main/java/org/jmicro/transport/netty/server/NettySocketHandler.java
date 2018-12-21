package org.jmicro.transport.netty.server;

import java.nio.ByteBuffer;

import org.jmicro.api.JMicroContext;
import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.codec.ICodecFactory;
import org.jmicro.api.idgenerator.IIdClient;
import org.jmicro.api.monitor.IMonitorDataSubmiter;
import org.jmicro.api.net.IMessageReceiver;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

@Component(lazy=false,side=Constants.SIDE_PROVIDER)
@Sharable
public class NettySocketHandler extends ChannelInboundHandlerAdapter {
	
    static final Logger logger = LoggerFactory.getLogger(NettySocketHandler.class);
	
	private static final AttributeKey<NettyServerSession> sessionKey = 
			AttributeKey.newInstance(Constants.SESSION_KEY+"Netty" + System.currentTimeMillis());
	
	@Cfg("/MinaServer/readBufferSize")
	private int readBufferSize = 1024*4;

	@Cfg("/MinaClientSessionManager/heardbeatInterval")
	private int heardbeatInterval = 3; //seconds to send heardbeat Rate
	
	@Cfg(value="/NettySocketHandler/openDebug",required=false)
	private boolean openDebug=false;
	
	@Inject("idClient")
	private IIdClient idGenerator;
	
	@Inject
	private ICodecFactory codeFactory;
	
	@Inject(required=false)
	private IMonitorDataSubmiter monitor;
	
	@Inject
	private IMessageReceiver receiver;
	
	@Cfg(value="/NettySocketHandler/dumpDownStream",defGlobal=false)
	private boolean dumpDownStream  = false;
	
	@Cfg(value="/NettySocketHandler/dumpUpStream",defGlobal=false)
	private boolean dumpUpStream  = false;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
    	
    	if(!(msg instanceof ByteBuf)) {
    		ctx.fireChannelRead(msg);
    		return;
    	}
    	
    	ByteBuf bb = (ByteBuf)msg;
    	if(bb.readableBytes() <= 0) {
    		return;
    	}
    	
    	ByteBuffer b = ByteBuffer.allocate(bb.readableBytes());
    	bb.readBytes(b);
    	b.flip();
    	bb.release();
    	
    	NettyServerSession session = ctx.channel().attr(sessionKey).get();
    	
    	JMicroContext.configProvider(monitor, session);
    	
    	session.receive(b);

    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    	super.handlerAdded(ctx);
    	NettyServerSession session = new NettyServerSession(ctx,readBufferSize,heardbeatInterval,
    			Constants.TYPE_SOCKET);
    	session.setReceiver(receiver);
    	session.setDumpDownStream(this.dumpDownStream);
    	session.setDumpUpStream(this.dumpUpStream);
    	ctx.channel().attr(sessionKey).set(session);
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	super.handlerRemoved(ctx);
    	ctx.channel().attr(sessionKey).remove();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    	logger.error("exceptionCaught close session",cause);
    	ctx.channel().attr(sessionKey).get().close(true);
    	ctx.channel().attr(sessionKey).remove();
    	ctx.close();
    }
    
}
