package com.poof.crawler.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Base64;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TestProxy {
	private static Logger log = Logger.getLogger(TestProxy.class);

	public static void main(String[] args) throws IOException, ParseException {
		System.err.println("55.2".matches(".*\\d+\\.?\\d.*"));
		if(1==1)return;
		InetSocketAddress address = new InetSocketAddress("198.46.206.196", 14807);
		Proxy proxy = new Proxy(Type.HTTP,address);
		Document doc = Jsoup.connect("http://www.ip138.com/ip2city.asp").proxy(proxy)
				.timeout(60 * 1000)
				.header("Proxy-Authorization", "Basic " + Base64.getEncoder().encodeToString(("17707605:6EBeTY4YM").getBytes()))
				.execute()
				.parse();
		System.err.println(doc.html());
		if(1==1)return;
		System.setProperty("http.maxRedirects", "50");
		System.getProperties().setProperty("proxySet", "true");
		System.getProperties().setProperty("http.proxyHost", "23.238.225.170");
		System.getProperties().setProperty("http.proxyPort", "65022");

		// 确定代理是否设置成功
		System.err.println(getHtml("http://www.ip138.com/ip2city.asp"));

	}

	private static String getHtml(String address) {
		StringBuffer html = new StringBuffer();
		String result = null;
		try {
			URL url = new URL(address);
			URLConnection conn = url.openConnection();
			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; NT 5.1; GTB5; .NET CLR 2.0.50727; CIBA)");
			BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
			try {
				String inputLine;
				byte[] buf = new byte[4096];
				int bytesRead = 0;
				while (bytesRead >= 0) {
					inputLine = new String(buf, 0, bytesRead, "ISO-8859-1");
					html.append(inputLine);
					bytesRead = in.read(buf);
					inputLine = null;
				}
				buf = null;
			} finally {
				in.close();
				conn = null;
				url = null;
			}
			result = new String(html.toString().trim().getBytes("ISO-8859-1"), "gb2312").toLowerCase();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			html = null;
		}
		return result;
	}
}
