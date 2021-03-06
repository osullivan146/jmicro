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
package cn.jmicro.api.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import cn.jmicro.api.codec.JDataInput;
import cn.jmicro.api.codec.JDataOutput;
import cn.jmicro.api.monitor.MC;
import cn.jmicro.common.CommonException;
import cn.jmicro.common.Constants;
import cn.jmicro.common.util.JsonUtils;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:44
 */
public final class Message {
	
	public static final int HEADER_LEN = 14;
	
	public static final byte PROTOCOL_BIN = 0;
	public static final byte PROTOCOL_JSON = 1;
	
	public static final byte PRIORITY_0 = 0;
	public static final byte PRIORITY_1 = 1;
	public static final byte PRIORITY_2 = 2;
	public static final byte PRIORITY_3 = 3;
	public static final byte PRIORITY_4 = 4;
	public static final byte PRIORITY_5 = 5;
	public static final byte PRIORITY_6 = 6;
	public static final byte PRIORITY_7 = 7;
	
	public static final byte PRIORITY_MIN = PRIORITY_0;
	public static final byte PRIORITY_NORMAL = PRIORITY_3;
	public static final byte PRIORITY_MAX = PRIORITY_7;
	
	public static final int MAX_SHORT_VALUE = ((int)Short.MAX_VALUE)*2;
	
	public static final short MAX_BYTE_VALUE = ((short)Byte.MAX_VALUE)*2;
	
	public static final long MAX_INT_VALUE = ((long)Integer.MAX_VALUE) *2;
	
	//public static final long MAX_LONG_VALUE = Long.MAX_VALUE*2;
	
	public static final byte MSG_VERSION = (byte)1;
	
	//长度字段类型，1表示整数，0表示短整数
    public static final short FLAG_LENGTH_INT = 1 << 0;
    
	//调试模式
	public static final short FLAG_DEBUG_MODE = 1 << 1;
	
	//需要响应的请求
	public static final short FLAG_NEED_RESPONSE = 1 << 2;
	
	public static final short FLAG_UP_PROTOCOL = 1<<5;
	
	public static final short FLAG_DOWN_PROTOCOL = 1 << 6;
	
	//DUMP上行数据
	public static final short FLAG_DUMP_UP = 1 << 7;
		
	//DUMP下行数据
	public static final short FLAG_DUMP_DOWN = 1 << 8;
	
	//可监控消息
	public static final short FLAG_MONITORABLE = 1 << 9;
	
	//异步请求响应类消息
	public static final short FLAG_ASYNC_RESUTN_RESULT = 1 << 13;
	
	//0B00111000 5---3
	//public static final short FLAG_LEVEL = 0X38;
	
	//是否启用服务级log
	//public static final short FLAG_LOGGABLE = 1 << 3;
	
	private transient long startTime = -1;
	
	//此消息所占字节数，用于记录流量
	private transient int len = -1;
	
	//1 byte length
	private byte version;
	
	//normal message ID	or JRPC request ID
	private long reqId;
	
	//payload length with byte,4 byte length
	//private int len;
	
	// 1 byte
	private byte type;
	
	/**
	 * 0        S:       data length type 0:short 1 : int
	 * 1        dm:      is development mode
	 * 2        N:       need Response
	 * 3,4      PP:      Message priority 
	 * 5        UPR:     up protocol  0:bin,  1: json 
     * 6        DPR:     down protocol 0:bin, 1 : json 
	 * 7        up:      dump up stream data
	 * 8        do:      dump down stream data
	 * 9        M:       Monitorable
	 * 10,11,12 LLL      Log level
	 * 13       A:       async return result，different from async RPC
	 * 
	          A   L  L   L   M  DO UP  DPR  UPR  P    P   N   dm   S
	 |    |   |   |  |   |   |  |  |   |    |    |    |   |    |   |
     15  14  13  12  11  10  9  8  7   6    5    4    3   2    1   0
     
	 * @return
	 */
	private short flag = 0;
	
	/**
	 * up: dump up stream data
	 * do: dump down stream data
	 * M: Monitorable
	 * L: 日志级别 
	   A: 异步返回结果，区别于异步RPC
	 * 
	 *   A L L L M  do up
	 * | | | | | |  |  |
	 * 7 6 5 4 3 2  1  0
	 * @return
	 */
	//private byte flag0 = 0;
	
	//request or response
	//private boolean isReq;
	
	//2 byte length
	//private byte ext;
	
	private Object payload;	
	
	//*****************development mode field begin******************//
	private long msgId;
	private long linkId;
	private long time;
	private String instanceName;
	private String method;
	
	//****************development mode field end*******************//
	
	public Message(){}
	
	public static boolean is(short flag, short mask) {
		return (flag & mask) != 0;
	}
	
	/*public static boolean is(byte flag, short mask) {
		return (flag & mask) != 0;
	}*/
	
	public boolean isAsyncReturnResult() {
		return is(flag,FLAG_ASYNC_RESUTN_RESULT);
	}
	
	public void setAsyncReturnResult(boolean f) {
		flag = set(f,flag,FLAG_ASYNC_RESUTN_RESULT);
	}
	
	public boolean isDumpUpStream() {
		return is(flag,FLAG_DUMP_UP);
	}
	
	public void setDumpUpStream(boolean f) {
		//flag0 |= f ? FLAG0_DUMP_UP : 0 ; 
		flag = set(f,flag,FLAG_DUMP_UP);
	}
	
	public boolean isDumpDownStream() {
		return is(flag,FLAG_DUMP_DOWN);
	}
	
	public void setDumpDownStream(boolean f) {
		flag = set(f,flag,FLAG_DUMP_DOWN);
	}
	
	public boolean isLoggable() {
		return this.getLogLevel() > MC.LOG_NO;
	}
	
	public boolean isDebugMode() {
		return is(flag,FLAG_DEBUG_MODE);
	}
	
	public void setDebugMode(boolean f) {
		flag = set(f,flag,FLAG_DEBUG_MODE);
	}
	
	public static short set(boolean isTrue,short f,short mask) {
		return isTrue ?(f |= mask) : (f &= ~mask);
	}
	
	public boolean isMonitorable() {
		return is(flag,FLAG_MONITORABLE);
	}
	
	public void setMonitorable(boolean f) {
		flag = set(f,flag,FLAG_MONITORABLE);
	}
	
	public boolean isNeedResponse() {
		return is(flag,FLAG_NEED_RESPONSE);
	}
	
	public void setNeedResponse(boolean f) {
		flag = set(f,flag,FLAG_NEED_RESPONSE);
	}
	
	/**
	 * 
	 * @param f true 表示整数，false表示短整数
	 */
	public void setLengthType(boolean f) {
		//flag |= f ? FLAG_LENGTH_INT : 0 ; 
		flag = set(f,flag,FLAG_LENGTH_INT);
	}
	
	public boolean isLengthInt() {
		return is(flag,FLAG_LENGTH_INT);
	}
	
	public int getPriority() {
		return (byte)((flag >>> 3) & 0x03);
	}
	
	public void setPriority(int l) {
		if(l > PRIORITY_3 || l < PRIORITY_0) {
			 new CommonException("Invalid priority: "+l);
		}
		this.flag = (byte)((l << 3) | this.flag);
	}
	
	public byte getLogLevel() {
		return (byte)((flag >>> 10) & 0x07);
	}
	//000 001 010 011 100 101 110 111
	public void setLogLevel(int v) {
		if(v < MC.LOG_NO || v > MC.LOG_FINAL) {
			 new CommonException("Invalid Log level: "+v);
		}
		this.flag = (byte)((v << 10) | this.flag);
	}
	
	public byte getUpProtocol() {
		return is(this.flag,FLAG_UP_PROTOCOL)?(byte)1:0;
	}

	public void setUpProtocol(byte protocol) {
		//flag |= protocol == PROTOCOL_JSON ? FLAG_UP_PROTOCOL : 0 ; 
		flag = set(protocol == PROTOCOL_JSON,flag,FLAG_UP_PROTOCOL);
	}
	
	public byte getDownProtocol() {
		return is(this.flag,FLAG_DOWN_PROTOCOL)?(byte)1:0;
	}

	public void setDownProtocol(byte protocol) {
		//flag |= protocol == PROTOCOL_JSON ? FLAG_DOWN_PROTOCOL : 0 ;
		flag = set(protocol == PROTOCOL_JSON,flag,FLAG_DOWN_PROTOCOL);
	}
	
	public static Message decode(JDataInput b) {
		try {
			Message msg = null;
			//第0个字节
			short flag = b.readShort();

			msg = new Message();
			msg.flag =  flag;
			//ByteBuffer b = ByteBuffer.wrap(data);
			int len = 0;
			if(is(flag,FLAG_LENGTH_INT)) {
				len = b.readInt();
			} else {
				len = b.readUnsignedShort(); // len = 数据长度 + 测试模式时附加数据长度
			}
			if(b.remaining() < len){
				throw new CommonException("Message len not valid");
			}
			
			//第3个字节
			msg.setVersion(b.readByte());
			
			//read type
			//第4个字节
			msg.setType(b.readByte());
			
			//第5，6，7，8个字节
			msg.setReqId(b.readInt());
			
			//第9，10，11，12个字节
			msg.setLinkId(b.readInt());
			
			//第13个字节
			//msg.flag0 = b.readByte();
			
			if(msg.isDebugMode()) {
				//读取测试数据头部
				msg.setId(b.readLong());
				msg.setTime(b.readLong());
				len -= 16;
				
				msg.setInstanceName(b.readUTF());
				msg.setMethod(b.readUTF());
				//减去测试数据头部长度
				len -= JDataOutput.encodeStringLen(msg.getInstanceName());
				len -= JDataOutput.encodeStringLen(msg.getMethod());
			}
			
			if(len > 0){
				byte[] payload = new byte[len];
				b.readFully(payload,0,len);
				msg.setPayload(ByteBuffer.wrap(payload));
			}else {
				msg.setPayload(null);
			}
			
			msg.setLen(len + Message.HEADER_LEN);
			
			return msg;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public ByteBuffer encode() {
		
		JDataOutput b = new JDataOutput(512);
		
		//ByteBuffer b =  null;
		
		boolean debug = this.isDebugMode();

		ByteBuffer data = null;
		if(this.getPayload() instanceof ByteBuffer) {
			data = (ByteBuffer)this.getPayload();
		} else {
			String json = JsonUtils.getIns().toJson(this.getPayload());
			try {
				data = ByteBuffer.wrap(json.getBytes(Constants.CHARSET));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		data.mark();
		
		int len = 0;//数据长度 + 测试模式时附加数据长度
		if(data != null){
			len = data.remaining();
		}
		
		if(debug) {
			len += JDataOutput.encodeStringLen(instanceName);
			len += JDataOutput.encodeStringLen(method);
			//2个long的长度，2*8=8
			len += 16;
		}
		
		//len += Message.HEADER_LEN
		
		//第1，2个字节 ,len = 数据长度 + 测试模式时附加数据长度
		if(len < MAX_SHORT_VALUE) {
			this.setLengthType(false);
			//b = ByteBuffer.allocate(len + Message.HEADER_LEN);
		} else if(len < MAX_INT_VALUE){
			this.setLengthType(true);
			//b = ByteBuffer.allocate(len + Message.HEADER_LEN+2);
		} else {
			throw new CommonException("Data length too long than :"+MAX_INT_VALUE+", but value "+len);
		}
		
		try {
			//第0,1个字节，标志头
			//b.put(this.flag);
			b.writeShort(this.flag);
			
			if(len < 65535) {
				//第2，3个字节 ,len = 数据长度 + 测试模式时附加数据长度
				//this.setLengthType(false);
				//writeUnsignedShort(b, len);
				b.writeUnsignedShort(len);
			}else if(len < Integer.MAX_VALUE){
				//消息内内容最大长度为MAX_VALUE 2,3,4,5
				//this.setLengthType(true);
				//b.putInt(len);
				b.writeInt(len);
			} else {
				throw new CommonException("Max int value is :"+ Integer.MAX_VALUE+", but value "+len);
			}
			
			//b.putShort((short)0);
			
			//第3个字节
			//b.put(this.version);
			b.writeByte(this.version);
			
			//第4个字节
			//writeUnsignedShort(b, this.type);
			//b.put(this.type);
			b.writeByte(this.type);
			
			//第5，6，7，8个字节
			//writeUnsignedInt(b, this.reqId);
			b.writeInt((int)reqId);
			
			//第9，10，11，12个字节
			//writeUnsignedInt(b, this.linkId);
			b.writeInt((int)linkId);
			
			//第13个字节
			//b.put(this.flag0);
			//b.writeByte(this.flag0);
			
			if(debug) {
				//b.putLong(this.getId());
				//b.putLong(this.getTime());
				b.writeLong(this.getId());
				b.writeLong(this.getTime());
				
				try {
					//JDataOutput.writeString(b, this.instanceName);
					//JDataOutput.writeString(b, this.method);
					b.writeUTF(this.instanceName);
					b.writeUTF(this.method);
				} catch (IOException e) {
					throw new CommonException("",e);
				}
				
				//OnePrefixTypeEncoder.encodeString(b, this.instanceName);
				//OnePrefixTypeEncoder.encodeString(b, this.method);
			}
			
			if(data != null){
				//b.put(data);
				b.write(data);
				data.reset();
			}
			
			//b.flip();
			ByteBuffer bb = b.getBuf();
			this.len = bb.limit();
			return bb;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Message readMessage(ByteBuffer cache){
		
		//保存读数据位置
		int pos = cache.position();
		
		//数据总长是否可构建一个包的最小长度
		int totalLen = cache.remaining();
		if(totalLen < Message.HEADER_LEN) {
			//可读的数据长度小于头部长度
			return null;
		}
		
		//取第一个字节标志位
		short f = cache.getShort();
		int len = 0;
		int headerLen = Message.HEADER_LEN;
		//取第二，第三个字节 数据长度
	    if(is(f,FLAG_LENGTH_INT)) {
	    	//数据长度不可能起过整数的最大值
	    	//len = cache.getInt();
	    	len = cache.getInt();
	    	//还原读数据公位置
			cache.position(pos);
			headerLen = headerLen + 2;
	    	if(totalLen < len + headerLen){
				//还不能构成一个足够长度的数据包
				return null;
			}
	    } else {
	    	len = Message.readUnsignedShort(cache);
	    	//还原读数据公位置
			cache.position(pos);
	    	if(totalLen < len + headerLen){
				//还不能构成一个足够长度的数据包
				return null;
			}
	    }
		
		byte[] data = new byte[len + headerLen];
		//从缓存中读一个包,cache的position往前推
		cache.get(data, 0, len+headerLen);
		
		return Message.decode(new JDataInput(ByteBuffer.wrap(data)));
        
	}
	
	public static void writeUnsignedShort(ByteBuffer b,int v) {
		if(v > MAX_SHORT_VALUE) {
    		throw new CommonException("Max short value is :"+MAX_SHORT_VALUE+", but value "+v);
    	}
		byte data = (byte)((v >>> 8) & 0xFF);
		b.put(data);
		data = (byte)((v >>> 0) & 0xFF);
		b.put(data);
	}
	
	 public static int readUnsignedShort(ByteBuffer b) {
		int firstByte = (0xFF & ((int)b.get()));
		int secondByte = (0xFF & ((int)b.get()));
		int anUnsignedShort  = (int) (firstByte << 8 | secondByte);
        return anUnsignedShort;
	 }
	
	public static void writeUnsignedByte(ByteBuffer b,short v) {
		if(v > MAX_BYTE_VALUE) {
    		throw new CommonException("Max byte value is :"+MAX_BYTE_VALUE+", but value "+v);
    	}
		byte vv = (byte)((v >>> 0) & 0xFF);
		b.put(vv);
	}
	
	public static short readUnsignedByte(ByteBuffer b) {
		short vv = (short) (b.get() & 0xff);
	    return vv;
	}
    
    public static long readUnsignedInt(ByteBuffer buf) {
    	/*int firstByte = (0xFF & ((int)b.get()));
    	int secondByte = (0xFF & ((int)b.get()));
    	int thirdByte = (0xFF & ((int)b.get()));
    	int fourthByte = (0xFF & ((int)b.get()));
 	    long anUnsignedInt  = 
 	    		((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte))
 	    		& 0xFFFFFFFFL;
 	    return anUnsignedInt;*/
    	
    	int b = buf.get() & 0xff;
		int n = b & 0x7f;
		if (b > 0x7f) {
			b = buf.get() & 0xff;
			n ^= (b & 0x7f) << 7;
			if (b > 0x7f) {
				b = buf.get() & 0xff;
				n ^= (b & 0x7f) << 14;
				if (b > 0x7f) {
					b = buf.get() & 0xff;
					n ^= (b & 0x7f) << 21;
					if (b > 0x7f) {
						b = buf.get() & 0xff;
						n ^= (b & 0x7f) << 28;
						if (b > 0x7f) {
							throw new CommonException("Invalid int encoding");
						}
					}
				}
			}
		}
		return (n >>> 1) ^ -(n & 1);
		
    }
    
    public static void writeUnsignedInt(ByteBuffer buf,long n) {
    	if(n > MAX_INT_VALUE) {
    		throw new CommonException("Max int value is :"+MAX_INT_VALUE+", but value "+n);
    	}
		/*b.put((byte)((v >>> 24)&0xFF));
		b.put((byte)((v >>> 16)&0xFF));
		b.put((byte)((v >>> 8)&0xFF));
		b.put((byte)((v >>> 0)&0xFF));*/
		
		n = (n << 1) ^ (n >> 31);
		if ((n & ~0x7F) != 0) {
			buf.put((byte) ((n | 0x80) & 0xFF));
			n >>>= 7;
			if (n > 0x7F) {
				buf.put((byte) ((n | 0x80) & 0xFF));
				n >>>= 7;
				if (n > 0x7F) {
					buf.put((byte) ((n | 0x80) & 0xFF));
					n >>>= 7;
					if (n > 0x7F) {
						buf.put((byte) ((n | 0x80) & 0xFF));
						n >>>= 7;
					}
				}
			}
		}
		buf.put((byte) n);
	}
    
	public int getLen() {
		return len;
	}

	public void setLen(int len) {
		this.len = len;
	}

	public long getId() {
		return this.msgId;
	}

	public void setId(long id) {
		this.msgId = id;
	}

	public byte getVersion() {
		return version;
	}
	public void setVersion(byte version) {
		this.version = version;
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}
	
	public short getFlag() {
		return flag;
	}

	public void setFlag(short flag) {
		this.flag = flag;
	}

	public Object getPayload() {
		return payload;
	}
	public void setPayload(Object payload) {
		this.payload = payload;
	}
	
	public Long getReqId() {
		return reqId;
	}

	public void setReqId(long reqId) {
		this.reqId = reqId;
	}

	public long getLinkId() {
		return linkId;
	}

	public void setLinkId(long linkId) {
		this.linkId = linkId;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getInstanceName() {
		return instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	@Override
	public String toString() {
		return "Message [version=" + version + ", msgId=" + msgId + ", reqId=" + reqId + ", linkId=" + linkId 
				+ ", type=" + type + ", flag=" + Integer.toHexString(flag)
				+ ", payload=" + payload + ", time="+ time 
				+ ", devMode=" + this.isDebugMode() + ", monitorable="+ this.isMonitorable() 
				+ ", needresp="+ this.isNeedResponse()
				+ ", upstream=" + this.isDumpUpStream() + ", downstream="+ this.isDumpDownStream() 
				+ ", instanceName=" + instanceName + ", method=" + method + "]";
	}

	
}
