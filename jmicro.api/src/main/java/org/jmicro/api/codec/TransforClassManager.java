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
package org.jmicro.api.codec;

import java.util.List;

import org.jmicro.api.IListener;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.idgenerator.ComponentIdServer;
import org.jmicro.api.raft.IDataOperator;
import org.jmicro.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Yulei Ye
 * @date 2018年11月16日 上午12:20:14
 *
 */
@Component(lazy=false)
public class TransforClassManager {

	static final Logger logger = LoggerFactory.getLogger(TransforClassManager.class);
	
	private static final String ROOT = Constants.CFG_ROOT+"/tclist";
	
	@Inject
	private IDataOperator dataOperator;
	
	@Inject
	private ComponentIdServer idGenerator;
	
	public void registType(Class<?> clazz,Short type) {
		String path = ROOT+"/"+clazz.getName();
		if(!dataOperator.exist(path)) {
			if(type == null || type == 0) {
				type = idGenerator.getIntId(TransforClassManager.class).shortValue();
			}
			dataOperator.createNode(path, type.toString(), false);
		}
	}
	
	
	public void init() {
		dataOperator.addChildrenListener(ROOT, (type,path,child,data)->{
			if(type == IListener.SERVICE_REMOVE) {
				//this.update(path,child,data);
			}else if (type == IListener.SERVICE_ADD) {
				this.update(path,child,data);
			}
			
		});
	}

	private void update(String parent,String child,String data) {

		Class<?> clazz;
		try {
			clazz = Thread.currentThread().getContextClassLoader().loadClass(child);
			if(Decoder.getType(clazz) == null ) {
				Decoder.registType(clazz,Short.parseShort(data));
			}
		} catch (ClassNotFoundException e) {
			logger.error("",e);
		}
	
	}
	
}
