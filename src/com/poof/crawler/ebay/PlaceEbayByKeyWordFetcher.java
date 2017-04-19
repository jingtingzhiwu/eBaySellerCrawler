package com.poof.crawler.ebay;

import java.net.URLEncoder;

import org.apache.log4j.Logger;

import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.dom.ListingParser;
import com.poof.crawler.utils.pool.KeyWordListPool;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月17日 上午09:12:16
 */
public class PlaceEbayByKeyWordFetcher  extends PlaceEbayFetcher implements Runnable{
	private static Logger log = Logger.getLogger(PlaceEbayByKeyWordFetcher.class);

	private Schedule schedule;
	
	public PlaceEbayByKeyWordFetcher(Schedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public void run() {

		log.info("starting [PlaceEbayByKeyWord] , searchterm: [" + schedule.getSearchTerm() + "]");

		try {
			KeyWordListPool.getInstance().execute(new ListingParser(schedule, String.format(PRE_URL, schedule.getSite(), URLEncoder.encode(String.format(ITEMID_LIST_URL, schedule.getSite(), schedule.getSearchTerm(), 1, 0), "UTF-8")) + END_URL));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
