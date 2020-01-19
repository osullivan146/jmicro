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
package org.jmicro.server;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Interceptor;
import org.jmicro.api.exception.RpcException;
import org.jmicro.api.net.AbstractInterceptor;
import org.jmicro.api.net.IInterceptor;
import org.jmicro.api.net.IRequest;
import org.jmicro.api.net.IRequestHandler;
import org.jmicro.api.net.IResponse;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:06:35
 */
@Component(value=Constants.LAST_INTERCEPTOR,lazy=false,side=Constants.SIDE_PROVIDER)
@Interceptor(order = Integer.MAX_VALUE)
public class LastInterceptor extends AbstractInterceptor implements IInterceptor {

	private final static Logger logger = LoggerFactory.getLogger(LastInterceptor.class);
	
	@Override
	public IResponse intercept(IRequestHandler handler, IRequest request) throws RpcException {
		//logger.debug("LastInterceptor before");
		IResponse resp = handler.onRequest(request);
		//logger.debug("LastInterceptor after");
		return resp;
	}
	
}
