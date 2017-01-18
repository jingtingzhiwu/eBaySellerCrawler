package com.poof.crawler.test;

import java.io.UnsupportedEncodingException;

import com.poof.crawler.utils.pool.KeyWordListPool;
import com.poof.crawler.utils.pool.ThreadPoolMirror;

public class test {
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
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
