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
package org.jmicro.api.monitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Beck Ye
 * @date 2018年10月4日-上午11:49:26
 */
public final class MonitorConstant {
	
	private MonitorConstant(){}

    public static final int CLIENT_INVALID  = 0X7FFFFFFF;
	//请求开始
	public static final int CLIENT_REQ_BEGIN  = 0X7FFFFFF1;
	//请求超时
	public static final int CLIENT_REQ_TIMEOUT = 0X7FFFFFF2;
	//请求的服务未找到，注册表里面没有相应的服务
	public static final int CLIENT_REQ_SERVICE_NOT_FOUND = 0X7FFFFFF3;
	//服务熔断
	public static final int CLIENT_REQ_SERVICE_BREAKING = 0X7FFFFFF4;
	//请好正常返回，包括异步请求正常返回的确认包
	public static final int CLIENT_REQ_OK = 0X7FFFFFF6;
	//请求超时，并且经过重试指定次数之后都没成功，返回失败
	public static final int CLIENT_REQ_TIMEOUT_FAIL = 0X7FFFFFFA;
	//请求服务存在，但是服务不存在此方法，此种情况应该永远不存在
	public static final int CLIENT_REQ_METHOD_NOT_FOUND = 0X7FFFFFF7;
	//public static final int CLIENT_REQ_HAVE_FINISH = 0X7FFFFFF8;
	//请求重试
	public static final int CLIENT_REQ_RETRY = 0X7FFFFFF9;
	//请求连接失败
	public static final int CLIENT_REQ_CONN_FAIL = 0X7FFFFFFB;
	//请求连接关闭
	public static final int CLIENT_REQ_CONN_CLOSE = 0X7FFFFFFC;
	//请求异常
	public static final int CLIENT_REQ_EXCEPTION_ERR = 0X7FFFFFF5;
	//请求业务错误
	public static final int CLIENT_REQ_BUSSINESS_ERR = 0X7FFFFFFD;
	
	public static final int CLIENT_REQ_ASYNC1_SUCCESS= 0X7FFFFFFE;
	public static final int CLIENT_REQ_ASYNC2_SUCCESS= 0X7FFFFFE1;
	public static final int CLIENT_REQ_ASYNC2_FAIL= 0X7FFFFFE2;
	
	public static final int CLIENT_IOSESSION_CLOSE = 0X7FFFFFFD;
	public static final int CLIENT_IOSESSION_OPEN = 0X7FFFFFFE;
	public static final int CLIENT_IOSESSION_IDLE = 0X7FFFFFE5;
	public static final int CLIENT_IOSESSION_WRITE = 0X7FFFFFE0;
	public static final int CLIENT_IOSESSION_READ =  0X7FFFFFE1;
	public static final int CLIENT_IOSESSION_EXCEPTION = 0X7FFFFFE2;
	public static final int CLIENT_PACKAGE_SESSION_ID_ERR = 0X7FFFFFE3;
	
	public static final int SERVER_REQ_BEGIN =      0X7FFFFEF1;
    public static final int SERVER_REQ_LIMIT_FORBIDON=0X7FFFFEF2;
	public static final int SERVER_REQ_LIMIT_OK =   0X7FFFFEF3;
	public static final int SERVER_REQ_TIMEOUT =    0X7FFFFEF4;
	public static final int SERVER_REQ_OK =         0X7FFFFEF5;
	public static final int SERVER_REQ_ERROR =      0X7FFFFEF6;
	
	public static final int SERVER_RESP_OK =        0X7FFFFEF7;
	public static final int SERVER_RESP_ERR =       0X7FFFFEF8;
	public static final int SERVER_PACKAGE_SESSION_ID_ERR = 0X7FFFFEF9;
	
	public static final int SERVER_START =           0X7FFFFEFA;
	public static final int SERVER_STOP =            0X7FFFFEFB;
	public static final int SERVER_REQ_SERVICE_NOT_FOUND = 0X7FFFFEFC;
	
	public static final int SERVER_IOSESSION_CLOSE = 0X7FFFFEFD;
	public static final int SERVER_IOSESSION_OPEN =  0X7FFFFEFE;
	public static final int SERVER_IOSESSION_IDLE =  0X7FFFFEEF;
	public static final int SERVER_IOSESSION_WRITE = 0X7FFFFEE0;
	public static final int SERVER_IOSESSION_READ =  0X7FFFFEE1;
    public static final int SERVER_IOSESSION_EXCEPTION=0X7FFFFEE2;
    
    public static final int LINKER_ROUTER_MONITOR = 0X7FFFFEE3;
    
    public static final byte LOG_TRANCE = 1;
    
    public static final byte LOG_DEBUG = 2;
    
    public static final byte LOG_INFO = 3;
    
    public static final byte LOG_WARN = 4;
    
    public static final byte LOG_ERROR = 5;
    
    public static final byte LOG_FINAL = 6;
    
    //总失败数所占请求数比率
	public static final int STATIS_FAIL_PERCENT = 1;
	
	//总请求数
	public static final int STATIS_TOTAL_REQ = 2;
	
	//总成功数
	public static final int STATIS_TOTAL_SUCCESS = 3;
	
	//总失败数
	public static final int STATIS_TOTAL_FAIL = 4;
	
	//总成功数所占比率
	public static final int STATIS_SUCCESS_PERCENT = 5;
	
	//超时数
	public static final int STATIS_TOTAL_TIMEOUT = 6;
	
	//超时百分比
	public static final int STATIS_TIMEOUT_PERCENT = 7;
	
	public static final int STATIS_TOTAL_RESP = 8;
	
	public static final Map<Integer,String> MONITOR_VAL_2_KEY = new HashMap<>();
	static {
		Field[] fs = MonitorConstant.class.getDeclaredFields();
		for(Field f: fs){
			if(!Modifier.isStatic(f.getModifiers()) || !(f.getName().startsWith("SERVER_") 
					|| f.getName().startsWith("CLIENT_") || f.getName().startsWith("STATIS_") 
					|| f.getName().startsWith("LINKER_ROUTER")) ){
				continue;
			}
			try {
				MONITOR_VAL_2_KEY.put(f.getInt(null), f.getName());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
}
