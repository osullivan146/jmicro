package cn.jmicro.api.mng;

import java.util.List;

import cn.jmicro.api.Resp;
import cn.jmicro.api.executor.ExecutorInfo;
import cn.jmicro.codegenerator.AsyncClientProxy;

@AsyncClientProxy
public interface IThreadPoolMonitor {

	Resp<List<ExecutorInfo>> serverList();
	
	Resp<List<ExecutorInfo>> getInfo(String key,String type);
	
}
