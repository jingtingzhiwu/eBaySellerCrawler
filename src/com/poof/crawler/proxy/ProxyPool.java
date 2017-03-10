package com.poof.crawler.proxy;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poof.crawler.utils.HttpStatus;

/**
 * @author <a href="mailto:admin@wilkey.vip">wilkey</a>
 * @version create at 2016年3月27日 10:47
 */
public class ProxyPool {

	private Logger logger = LoggerFactory.getLogger(ProxyPool.class);

	private Vector<HttpProxy> idleQueue = new Vector<HttpProxy>(); // 存储空闲的Proxy
	private Map<String, HttpProxy> totalQueue = new ConcurrentHashMap<String, HttpProxy>(); // 存储所有的Proxy

	/**
	 * 添加Proxy
	 *
	 * @param httpProxies
	 */
	public void add(HttpProxy... httpProxies) {
		for (HttpProxy httpProxy : httpProxies) {
			if (totalQueue.containsKey(httpProxy.getKey())) {
				continue;
			}
			httpProxy.success();
			idleQueue.add(httpProxy);
			totalQueue.put(httpProxy.getKey(), httpProxy);
		}
	}

	public void add(String address, int port, String username, String password) {
		this.add(new HttpProxy(address, port, username, password));
	}

	public void add(String address, int port) {
		this.add(new HttpProxy(address, port));
	}

	/**
	 * 得到Proxy
	 *
	 * @return
	 */
	public HttpProxy borrow() {
		HttpProxy httpProxy = null;
		httpProxy = idleQueue.get(new Random().nextInt(idleQueue.size()));

		HttpProxy p = totalQueue.get(httpProxy.getKey());
		p.borrow();
		return httpProxy;
	}

	/**
	 * 反馈 Proxy
	 *
	 * @param httpProxy
	 * @param httpStatus
	 */
	public void reback(HttpProxy httpProxy, HttpStatus httpStatus) {
		switch (httpStatus) {
		case SC_OK:
			httpProxy.success();
			httpProxy.setReuseTimeInterval(HttpProxy.DEFAULT_REUSE_TIME_INTERVAL);
			break;
		case SC_FORBIDDEN:
			httpProxy.fail(httpStatus);
			httpProxy.setReuseTimeInterval(HttpProxy.DEFAULT_REUSE_TIME_INTERVAL * httpProxy.getFailedNum()); // 被网站禁止，调节更长时间的访问频率
			logger.info(httpProxy.getProxy() + " >>>> reuseTimeInterval is >>>> " + TimeUnit.SECONDS.convert(httpProxy.getReuseTimeInterval(), TimeUnit.MILLISECONDS));
			break;
		default:
			httpProxy.fail(httpStatus);
			break;
		}
		if (httpProxy.getFailedNum() > 5) { // 失败超过 5 次，移除代理池队列
			httpProxy.setReuseTimeInterval(HttpProxy.FAIL_REVIVE_TIME_INTERVAL);
			logger.error("remove proxy >>>> " + httpProxy.getProxy() + ">>>>" + httpProxy.countErrorStatus() + " >>>> remain proxy >>>> " + idleQueue.size());
			return;
		}
	}

	public void allProxyStatus() {
		String re = "all proxy info >>>> \n";
		for (Entry<String, HttpProxy> entry : totalQueue.entrySet()) {
			re += entry.getValue().toString() + "\n";
		}
		logger.info(re);
	}

	/**
	 * 获取当前空闲的Proxy
	 *
	 * @return
	 */
	public int getIdleNum() {
		return idleQueue.size();
	}

}
