package com.poof.crawler.test;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.poof.crawler.utils.pool.KeyWordListPool;
import com.poof.crawler.utils.pool.ThreadPoolMirror;

public class test {
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		String test1 = "515.46 Trending at RMB 755.94";

		   Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");  
		   Matcher matcher = pattern.matcher(test1);  
		  System.err.println(matcher.find());
		System.err.println(matcher.group(1));
		
		if(1==1)return;
		for (int i = 0; i < 5; i++) {
			final int c = i;
			KeyWordListPool.getInstance().execute(new Runnable() {
				
				@Override
				public void run() {
					System.err.println(c);
					try {
						Thread.sleep(5 * 1000);
						System.err.println("done. " + c);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		System.err.println("ok?");
		while(KeyWordListPool.getInstance().getCompletedTaskCount() != 5){
			System.err.println(KeyWordListPool.getInstance().getCompletedTaskCount());
			Thread.sleep(1000);
		}
		System.err.println(ThreadPoolMirror.dumpThreadPool("爬虫线程池", KeyWordListPool.getInstance()));
	}
}
