package com.poof.crawler.proxy;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * 这个DEMO主要为了测试动态代理IP的稳定性 也可以作为爬虫参考项目，如需使用，请自行修改代码webParseHtml方法
 */
public class TestDynamicIp {
	public static List<String> ipList = new ArrayList<String>();
	public static boolean gameOver = false;

	public static void main(String[] args) {
		long fetchIpSeconds = 5;
		int threadNum = 10;
		int testTime = 3;
		// 请填写无忧代理IP订单号，填写之后才可以提取到IP哦
		String order = "df53b80213e148bc99ffad40294ec487";
		// 你要抓去的目标网址
		String targetUrl = "http://1212.ip138.com/ic.asp";
		// 是否加载JS，加载JS会导致速度变慢
		boolean useJS = false;
		// 请求超时时间，单位毫秒，默认5秒
		int timeOut = 5000;

		if (order == null || "".equals(order)) {
			System.err.println("请输入无忧代理IP动态代理订单号");
			return;
		}

		System.out.println("############无忧代理动态IP测试开始###############");
		System.out.println("***************");
		System.out.println("接口返回IP为国内各地区，每次最多返回10个");
		System.out.println("提取IP间隔 " + fetchIpSeconds + " 秒 ");
		System.out.println("开启爬虫线程 " + threadNum);
		System.out.println("爬虫目标网址  " + targetUrl);
		System.out.println("测试次数 3 ");
		System.out.println("***************\n");
		TestDynamicIp tester = new TestDynamicIp();
		new Thread(tester.new GetIP(fetchIpSeconds * 1000, testTime, order)).start();
		for (int i = 0; i < threadNum; i++) {
			tester.new Crawler(100, targetUrl, useJS, timeOut).start();
		}
		while (!gameOver) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("###############无忧代理动态IP测试结束###############");
		System.exit(0);
	}

	// 抓取目标站，检测IP
	public class Crawler extends Thread {
		@Override
		public void run() {
			while (!gameOver) {
				webParseHtml(targetUrl);
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		long sleepMs = 200;
		boolean useJs = false;
		String targetUrl = "";
		int timeOut = 5000;

		public Crawler(long sleepMs, String targetUrl, boolean useJs, int timeOut) {
			this.sleepMs = sleepMs;
			this.targetUrl = targetUrl;
			this.useJs = useJs;
			this.timeOut = timeOut;
		}

		public String webParseHtml(String url) {
			String html = "";
			BrowserVersion[] versions = { BrowserVersion.INTERNET_EXPLORER_11, BrowserVersion.CHROME, BrowserVersion.FIREFOX_38, BrowserVersion.FIREFOX_38 };
			WebClient client = new WebClient(versions[(int) (versions.length * Math.random())]);
			try {
				client.getOptions().setThrowExceptionOnFailingStatusCode(false);
				client.getOptions().setJavaScriptEnabled(useJs);
				client.getOptions().setCssEnabled(false);
				client.getOptions().setThrowExceptionOnScriptError(false);
				client.getOptions().setTimeout(timeOut);
				client.getOptions().setAppletEnabled(true);
				client.getOptions().setGeolocationEnabled(true);
				client.getOptions().setRedirectEnabled(true);
				// 这行代码允许访问HTTPS网站，异常参考http://www.data5u.com/help/article-31.html
				client.getOptions().setUseInsecureSSL(true);

				String ipport = getAProxy();
				if (ipport != null) {
					ProxyConfig proxyConfig = new ProxyConfig(ipport.split(":")[0], Integer.parseInt(ipport.split(":")[1]));
					client.getOptions().setProxyConfig(proxyConfig);
				} else {
					System.out.print(".");
					return "";
				}

				HtmlPage page = client.getPage(url);
				html = page.asXml();

				System.out.println(getName() + " 使用代理 " + ipport + "请求目标网址返回HTML：" + html);

			} catch (Exception e) {
				return webParseHtml(url);
			} finally {
				client.close();
			}
			return html;
		}

		private String getAProxy() {
			if (ipList.size() > 0) {
				String ip = ipList.get((int) (Math.random() * ipList.size()));
				return ip;
			}
			return null;
		}
	}

	// 定时获取动态IP
	public class GetIP implements Runnable {
		long sleepMs = 1000;
		int maxTime = 3;
		String order = "";

		public GetIP(long sleepMs, int maxTime, String order) {
			this.sleepMs = sleepMs;
			this.maxTime = maxTime;
			this.order = order;
		}

		@Override
		public void run() {
			long getIpTime = 0;
			int time = 1;
			while (!gameOver) {
				if (time >= 4) {
					gameOver = true;
					break;
				}
				try {
					java.net.URL url = new java.net.URL("http://api.ip.data5u.com/dynamic/get.html?order=" + order + "&ttl");
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setConnectTimeout(3000);
					connection = (HttpURLConnection) url.openConnection();

					InputStream raw = connection.getInputStream();
					InputStream in = new BufferedInputStream(raw);
					byte[] data = new byte[in.available()];
					int bytesRead = 0;
					int offset = 0;
					while (offset < data.length) {
						bytesRead = in.read(data, offset, data.length - offset);
						if (bytesRead == -1) {
							break;
						}
						offset += bytesRead;
					}
					in.close();
					raw.close();
					String[] res = new String(data, "UTF-8").split("\n");
					List ipList = new ArrayList<>();
					for (String ip : res) {
						try {
							String[] parts = ip.split(",");
							if (Integer.parseInt(parts[1]) > 0) {
								ipList.add(parts[0]);
							}
						} catch (Exception e) {
						}
					}
					if (ipList.size() > 0) {
						TestDynamicIp.ipList = ipList;
						System.out.println("第" + ++getIpTime + "次获取动态IP " + ipList.size() + " 个");
						time += 1;
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println(">>>>>>>>>>>>>>获取IP出错");
				}
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}