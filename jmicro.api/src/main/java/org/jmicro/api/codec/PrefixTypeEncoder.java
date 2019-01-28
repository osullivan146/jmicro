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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jmicro.api.annotation.Cfg;
import org.jmicro.api.annotation.Component;
import org.jmicro.api.codec.typecoder.TypeCoder;
import org.jmicro.common.CommonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yulei Ye
 * @date 2018年10月4日-下午12:01:25
 */
@Component(value="prefixTypeEncoder",lazy=false)
public class PrefixTypeEncoder implements IEncoder<ByteBuffer>{

	private static final Logger logger = LoggerFactory.getLogger(PrefixTypeEncoder.class);
	
	@Cfg(value="/OnePrefixTypeEncoder",defGlobal=true,required=true)
	private int encodeBufferSize = 4092;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ByteBuffer encode(Object obj) {
		if(obj == null) {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			buffer.put(Decoder.PREFIX_TYPE_NULL);
			//空值直接返回
			return buffer;
		} 

		//buffer = ByteBuffer.allocate(encodeBufferSize);
		//入口从Object的coder开始
		TypeCoder coder = TypeCoderFactory.getDefaultCoder();
		//field declare as Object.class in order to put type info any way
		//从此进入时,字段声明及泛型类型都是空,区别于从反射方法进入
		ByteArrayOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new ByteArrayOutputStream();
			dos = new DataOutputStream(bos);
			coder.encode(dos, obj, null,null);
			byte[] data = bos.toByteArray();
			//byte[] temp = new byte[data.length];
			//System.arraycopy(data, 0, temp, 0, data.length);
			return  ByteBuffer.wrap(data);
		} catch (IOException e) {
			throw new CommonException("encode error:"+obj.toString(),e);
		}finally {
			if(bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					logger.error("encode",e);
				}
			}
			if(dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
					logger.error("encode",e);
				}
			}
		}
	}
	
}