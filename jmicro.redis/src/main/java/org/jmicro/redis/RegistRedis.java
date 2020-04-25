package org.jmicro.redis;

import org.jmicro.api.annotation.Component;
import org.jmicro.api.annotation.Inject;
import org.jmicro.api.config.Config;
import org.jmicro.api.objectfactory.IObjectFactory;
import org.jmicro.api.objectfactory.IPostFactoryListener;
import org.jmicro.common.util.StringUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component(lazy=false, level=0)
public class RegistRedis implements IPostFactoryListener{

	//@Cfg(value = "/RegistRedis/redisHost", defGlobal=true)
	private String redisHost = "127.0.0.1";
	
	//@Cfg(value = "/RegistRedis/port", defGlobal=true)
	private int port = 6379;
	
	@Inject
	private IObjectFactory of;
	
	public void init() {
	}

	@Override
	public void preInit(IObjectFactory of) {
		Config cfg = of.get(Config.class);
		String rh = cfg.getString("/RegistRedis/redisHost", "127.0.0.1");
		if(StringUtils.isNotEmpty(rh)) {
			redisHost = rh;
			//throw new CommonException("Redis config [/RegistRedis/redisHost] not found");
		}
		int p = cfg.getInt("/RegistRedis/port", 6379);
		if(p > 0) {
			port = p;
			//throw new CommonException("Redis config [/RegistRedis/redisHost] not found");
		}
		
		Jedis jedis = new Jedis(redisHost,port);
		of.regist(Jedis.class, jedis);
		
		JedisPool pool = new JedisPool(new JedisPoolConfig(), redisHost,port);
		of.regist(JedisPool.class, pool);
		
	}

	@Override
	public void afterInit(IObjectFactory of) {
		
	}

	@Override
	public int runLevel() {
		return 0;
	}
	
}
