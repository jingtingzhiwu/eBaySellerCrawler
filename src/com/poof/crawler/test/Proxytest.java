package com.poof.crawler.test;

import java.io.IOException;
import java.util.Base64;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

public class Proxytest {
	public static void main(String[] args) {
		Connection conn = Jsoup.connect("http://www.baidu.com").proxy("162.219.224.224",29842);

		try {
			Response response = conn.execute();
			System.err.println(response.body());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
