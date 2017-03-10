package com.poof.crawler.ebay;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.dom.ListingParser;
import com.poof.crawler.utils.pool.KeyWordListPool;

/**
 * @author wilkey
 * @mail admin@wilkey.vip
 * @Date 2017年2月4日 下午2:23:32
 */
public class PlaceEbayByItemIdFetcher extends PlaceEbayFetcher implements Runnable {
	private static Logger log = Logger.getLogger(PlaceEbayByItemIdFetcher.class);

	private Schedule schedule;

	public PlaceEbayByItemIdFetcher(Schedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public void run() {

		log.info("starting [PlaceEbayByItemId] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]");

		try {
			String[] itemids = schedule.getSearchTerm().replaceAll("\\s*|\\t|\\r|\\n", "").split(",");
			for (int i = 0; i < itemids.length; i++) {
				final int index = i;
				if (StringUtils.isBlank(itemids[i]))
					continue;
				KeyWordListPool.getInstance().execute(new ListingParser(schedule, String.format(ITEMID_LIST_URL, schedule.getSite(), itemids[index].trim(), 1, 0), getUSProxyHost()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
