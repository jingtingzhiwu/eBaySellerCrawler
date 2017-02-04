package com.poof.crawler.ebay;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.db.entity.ProxyHost;

/**
 * @author wilkey 
 * @mail admin@wilkey.vip
 * @Date 2017年1月18日 上午10:47:18
 */
public class PlaceEbayFetcher {

	private static Logger log = Logger.getLogger(PlaceEbayFetcher.class);

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
	protected static final Integer[] BYKEYWORD_MAX_PAGES = new Integer[] { 1 };
	protected final int BYSELLERID_MAX_PAGE = 50;
	protected String PRE_URL = "http://www.%s/sch/FindingCustomization/?_fcsp=%s";
	protected String END_URL = "&_fcsbm=1&_pppn=v3&_fcdm=1&_fcss=12&_fcps=1&_fcippl=4&_fcso=1&_fcpe=3%7C2%7C4&_fcie=36%7C1&_fcse=42%7C43%7C10&_fcpd=0";
	protected String ITEMID_LIST_URL = "http://www.%s/sch/i.html?_from=R40&_sacat=0&_sop=12&_nkw=%s&_pgn=%s&_skc=%s&rt=nc";
	protected String SELLERID_LIST_URL = "http://www.%s/sch/m.html?_ipg=200&_pgn=%s&_sop=12&_ssn=%s&_sac=1#seeAllAnchorLink";

	protected List<ProxyHost> getProxyHost() {
		try {
			List<ProxyHost> list = DBUtil.queryBeanList(DBUtil.openConnection(), "select * from t_proxy_host", ProxyHost.class);
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
		return null;
	}
}
