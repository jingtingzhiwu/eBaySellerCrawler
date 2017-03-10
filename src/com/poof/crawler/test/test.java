package com.poof.crawler.test;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.utils.pool.KeyWordListPool;
import com.poof.crawler.utils.pool.ThreadPoolMirror;

public class test {
	public static void main(String[] args) throws UnsupportedEncodingException, InterruptedException {
		

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

		if(1==1) return;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat ensdf = new SimpleDateFormat("MMM-d-yy HH:mm:ss",Locale.US);
			String[] a = new String[]{
					"Feb-22-17 21:18:50 PST",
					"Feb-22-17 14:58:46 PDT",
					"Feb-22-17 09:37:19 PST",
			};
			Connection conn = DBUtil.openConnection();
			PreparedStatement pstmt = conn.prepareStatement("insert into temp (tsd) values(?)");
			for (int i = 0; i < a.length; i++) {
				pstmt.setTimestamp(1, new Timestamp(ensdf.parse(a[i]).getTime()));
				System.err.println(sdf.format(ensdf.parse(a[i])));
				pstmt.execute();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.err.println("US $38.93".replaceAll("[^\\d.]", ""));
		if(1==1)return;
		/*String a = "_ipg%3D200%26_sop%3D12%26_ssn%3Dextra-deals4all%26_pgn%3D1%26_skc%3D0%26_sac%3D1%23";
		System.err.println(a.replaceAll("%26_pgn%3D1", "xxxxxx"));
		if(1==1)return;
		String test1 = "515.46 Trending at RMB 755.94";

		Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
		Matcher matcher = pattern.matcher(test1);
		System.err.println(matcher.find());
		System.err.println(matcher.group(1));*/
	}
}
