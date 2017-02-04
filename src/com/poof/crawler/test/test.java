package com.poof.crawler.test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

import com.poof.crawler.utils.pool.KeyWordListPool;
import com.poof.crawler.utils.pool.ThreadPoolMirror;

public class test {
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		/*String a = "_ipg%3D200%26_sop%3D12%26_ssn%3Dextra-deals4all%26_pgn%3D1%26_skc%3D0%26_sac%3D1%23";
		System.err.println(a.replaceAll("%26_pgn%3D1", "xxxxxx"));
		if(1==1)return;
		String test1 = "515.46 Trending at RMB 755.94";

		Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
		Matcher matcher = pattern.matcher(test1);
		System.err.println(matcher.find());
		System.err.println(matcher.group(1));*/
		for (int i = 0; i < 50; i++) {
			final int c = i;
			KeyWordListPool.getInstance().execute(new Runnable() {
				
				@Override
				public void run() {
					System.err.println(c);
					try {
						Thread.sleep(5 * 1000);
						System.err.println("done. " + c);
						System.err.println(ThreadPoolMirror.dumpThreadPool("爬虫线程池", KeyWordListPool.getInstance()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
