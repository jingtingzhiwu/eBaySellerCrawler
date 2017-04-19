package com.poof.crawler.ebay;

import java.util.Random;

/**
 * @author wilkey
 * @mail admin@wilkey.vip
 * @Date 2017年1月18日 上午10:47:18
 */
public class PlaceEbayFetcher {

	/**
	 * @param PRE_URL
	 *            自定义显示选项URL ITEMID_LIST_URL List页面参数URL SELLERID_LIST_URL
	 *            根据卖家ID查询URL
	 * 
	 * @desc ebay最多显示10000条 每页200条 根据关键字取前1页，即前200条
	 */
	public static final String ACCEPTED = "ACCEPTED";
	public static final String EXPIRED = "EXPIRED";
	public static final String DECLINED = "DECLINED";
	public static final String PENDING = "PENDING";
	public static String OFFER_DETAIL_URL = "http://offer.%s/ws/eBayISAPI.dll?ViewBidsLogin&item=%s&rt=nc&_trksid=p2047675.l2564";
	public static final Integer[] BYKEYWORD_MAX_PAGES = new Integer[] { 1 };
	public final int BYSELLERID_MAX_PAGE = 50;
	public String PRE_URL = "http://www.%s/sch/FindingCustomization/?_fcsp=%s";
	public String END_URL = "&_fcsbm=1&_pppn=v3&_fcdm=1&_fcss=12&_fcps=1&_fcippl=4&_fcso=1&_fcpe=3%7C2%7C4&_fcie=36%7C1&_fcse=42%7C43%7C10&_fcpd=0";
	public String ITEMID_LIST_URL = "http://www.%s/sch/i.html?_from=R40&_sacat=0&_sop=12&_nkw=%s&_pgn=%s&_skc=%s&rt=nc";
	public String SELLERID_LIST_URL = "http://www.%s/sch/m.html?_ipg=200&_sop=12&_ssn=%s&_pgn=%s&_skc=0&_sac=1#seeAllAnchorLink";
	private static String chars = "abcdefghijklmnopqrstuvwxyz1234567890_";
	public static String REFER_LIST_URL = "http://www.ebay.com/sch/i.html?_from=R40&_trksid=p2050601.m570.l1313.TR0.TRC0.H0.Xssd.TRS0&_nkw=" + randomWord() + "&_sacat=0";
	public static String REFER_OFFER_URL = "http://www.ebay.com/sch/i.html?_from=R40&_trksid=p2050601.m570.l1313.TR0.TRC0.H0.Xssd.TRS0&_nkw=" + randomWord() + "&_sacat=0";

	private static String randomWord() {
		String word = "";
		for (int i = 0; i < new Random().nextInt(20) + 5; i++) {
			word += chars.charAt((int) (Math.random() * 26));
		}
		return word;
	}

	public static void main(String[] args) {
		System.err.println(new PlaceEbayFetcher().randomWord());
	}
}
