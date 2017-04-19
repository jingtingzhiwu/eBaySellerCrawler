package com.poof.crawler.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class TestEbayCookie {
	public static void main(String[] args) {

		WebClient webClient = new WebClient();
		webClient.getCookieManager().setCookiesEnabled(true);
		webClient.getOptions().setTimeout(10000);
		webClient.getCookieManager().clearCookies();
		webClient.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
		webClient.addRequestHeader("Accept-Encoding", "gzip, deflate");
		webClient.addRequestHeader("Accept-Language", "zh-CN,zh;q=0.8");
		try {
			Map<String, String> cookieMap = new HashMap<String, String>();

			UnexpectedPage jsonpage = webClient.getPage(
					"http://www.ebay.com/itm/getrates?item=361436378134&_trksid=p2047675.l2682&quantity=1&country=1&zipCode=123456&co=0&cb=jQuery1707099015460951956_1490597642391&_=1490597831645");
			webClient.waitForBackgroundJavaScript(30000);
			Set<Cookie> set = webClient.getCookieManager().getCookies();
			for (Iterator<Cookie> iterator = set.iterator(); iterator.hasNext();) {
				Cookie cookie = iterator.next();
				cookieMap.put(cookie.getName(), cookie.getValue());
				System.err.println("Cookie: -----------" + cookie.getName() + " : " + cookie.getValue());
			}
			
			cookieMap.clear();
			cookieMap.put("nonsession", "BAQAAAVsCwzFDAAaAADMACVq5798xMjM0NSxVU0EAywABWNjDZzEAygAgYj693zBlOWZkMzBlMTViMGE4NjBhMTg1N2I0M2ZmZDI1M2UwgO2Cp3aKRaOUxHraqtQHaV/DAqQ*");
			Response response = Jsoup.connect("http://www.ebay.com/sch/factorydirectsale/m.html?_ipg=50&_sop=12&_rdc=1")
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.98 Safari/537.36")
					.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8").header("Accept-Encoding", "gzip, deflate, sdch")
					.header("Accept-Language", "zh-CN,zh;q=0.8").cookies(cookieMap).timeout(1000 * 60).execute();
			Document doc = response.parse();

			System.err.println(doc.html());
			System.err.println("count: " + doc.select(".rcnt").text());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
