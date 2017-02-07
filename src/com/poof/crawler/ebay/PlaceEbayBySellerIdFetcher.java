package com.poof.crawler.ebay;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.poof.crawler.db.entity.ProxyHost;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.dom.ListingParser;
import com.poof.crawler.utils.pool.SellerIDListPool;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月18日 上午10:46:51
 */
public class PlaceEbayBySellerIdFetcher  extends PlaceEbayFetcher implements Runnable{
	private static Logger log = Logger.getLogger(PlaceEbayBySellerIdFetcher.class);

	private Schedule schedule;
	
	public PlaceEbayBySellerIdFetcher(Schedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public void run() {

		log.info("starting [PlaceEbayBySellerId] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]");
		List<ProxyHost> proxies = getProxyHost();
		if(proxies != null)
		Collections.shuffle(proxies);
		
		try {
			TimeUnit.SECONDS.sleep(new Random().nextInt(20));
			SellerIDListPool.getInstance().execute(new ListingParser(schedule, String.format(PRE_URL, schedule.getSite(), URLEncoder.encode(String.format(SELLERID_LIST_URL, schedule.getSite(), schedule.getSearchTerm(), 1), "UTF-8")) + END_URL, proxies != null && proxies.size() > 0 ? proxies .get(0) : null));
			if (proxies != null)
				proxies.remove(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
