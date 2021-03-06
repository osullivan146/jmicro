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
package org.jmicro.transport.mina;

import java.net.InetSocketAddress;

import org.apache.mina.api.IoSession;
import org.jmicro.api.net.AbstractSession;
import org.jmicro.api.net.ISession;
/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:13:27
 */
public abstract class AbstractMinaSession extends AbstractSession implements ISession{

	private IoSession ioSession;
	
	public AbstractMinaSession(IoSession ioSession,int readBufferSize,int heardbeatInterval) {
		super(readBufferSize,heardbeatInterval);
		this.ioSession = ioSession;
	}
	
	public IoSession getIoSession() {
		return ioSession;
	}
	
	public void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}
	
	@Override
	public void close(boolean flag) {
	    super.close(flag);
		this.ioSession.close(true);
	}
	
	public boolean isClose(){
		return this.getIoSession().isClosed() || this.getIoSession().isClosing();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return (InetSocketAddress)ioSession.getLocalAddress();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress)ioSession.getRemoteAddress();
	}
}
