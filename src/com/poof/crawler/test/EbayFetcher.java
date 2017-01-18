package com.poof.crawler.test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.db.entity.ProxyHost;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.dom.ListingParser;
import com.poof.crawler.utils.pool.KeyWordListPool;
import com.poof.crawler.utils.pool.SellerIDListPool;

/**
 * @author wilkey
 * @mail admin@wilkey.vip
 * @Date 2017年1月10日 下午4:25:00
 */
public class EbayFetcher {
	private static Logger log = Logger.getLogger(EbayFetcher.class);

	/**
	 * @param
	 * 	PRE_URL					自定义显示选项URL
	 * 	ITEMID_LIST_URL		List页面参数URL
	 * 	SELLERID_LIST_URL	根据卖家ID查询URL
	 * 
	 * @desc
	 * ebay最多显示10000条
	 * 每页200条
	 * 根据关键字取前1页，即前200条
	 */
	private static final Integer[] BYKEYWORD_MAX_PAGES = new Integer[] { 1 };
	static final int BYSELLERID_MAX_PAGE = 50;
	static String PRE_URL = "http://www.%s/sch/FindingCustomization/?_fcsp=%s";
	static String END_URL = "&_fcsbm=1&_pppn=v3&_fcdm=1&_fcss=12&_fcps=1&_fcippl=4&_fcso=1&_fcpe=3%7C2%7C4&_fcie=36%7C1&_fcse=42%7C43%7C10&_fcpd=0";
	static String ITEMID_LIST_URL = "http://www.%s/sch/i.html?_from=R40&_sacat=0&_sop=12&_nkw=%s&_pgn=%s&_skc=%s&rt=nc";
	static String SELLERID_LIST_URL = "http://www.%s/sch/m.html?_ipg=200&_pgn=%s&_sop=12&_ssn=%s&_sac=1#seeAllAnchorLink";

	public static void main(String[] args) {
		System.err.println("starting......");
		Schedule s = new Schedule();
		s.setId("1");
		s.setName("test");
		s.setSearchTerm("towing mirrors");
		s.setSite("ebay.com");
		
		try {
			PlaceEbayByKeyWord(s);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public static void PlaceEbayByKeyWord(Schedule schedule) throws UnsupportedEncodingException {
		log.info("starting [PlaceEbayByKeyWord] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]");
		List<ProxyHost> proxies = getProxyHost();
		Collections.shuffle(proxies);

		for (int i = 1; i <= BYKEYWORD_MAX_PAGES.length; i++) {
			KeyWordListPool.getInstance().execute(new ListingParser(schedule, String.format(PRE_URL, schedule.getSite(), URLEncoder.encode(String.format(ITEMID_LIST_URL, schedule.getSite(), schedule.getSearchTerm(), i, 200 * i - 200), "UTF-8")) + END_URL, proxies.get(0)));
			proxies.remove(0);
		}
	}

	public static void PlaceEbayBySellerId(Schedule schedule) throws UnsupportedEncodingException {
		log.info("starting [PlaceEbayBySellerId] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]");
		List<ProxyHost> proxies = getProxyHost();
		Collections.shuffle(proxies);

		for (int i = 1; i <= BYKEYWORD_MAX_PAGES.length; i++) {
			SellerIDListPool.getInstance().execute(new ListingParser(schedule, String.format(PRE_URL, schedule.getSite(), URLEncoder.encode(String.format(SELLERID_LIST_URL, schedule.getSite(), schedule.getSearchTerm(), i), "UTF-8")) + END_URL, proxies.get(0)));
			proxies.remove(0);
		}
	}

	static List<ProxyHost> getProxyHost() {
		try {
			List<ProxyHost> list = DBUtil.queryBeanList(DBUtil.openConnection(), "select * from t_porxy_host", ProxyHost.class);
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		} finally {
			try {
				DBUtil.closeConnection();
			} catch (SQLException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
		}
		return new ArrayList<ProxyHost>();
	}
}