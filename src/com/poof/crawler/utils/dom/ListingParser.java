package com.poof.crawler.utils.dom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.ebay.PlaceEbayFetcher;
import com.poof.crawler.proxy.DynamicIp;
import com.poof.crawler.proxy.HttpProxy;
import com.poof.crawler.proxy.ProxyPool;
import com.poof.crawler.utils.pool.OfferPool;
import com.poof.crawler.utils.pool.ThreadPoolMirror;

public class ListingParser extends Parser implements Runnable {
	private static final String IMG_SELECTOR = "img.img";
	private static final String TITLE_SELECTOR = "[class*=lvtitle] a";
	private static final String SELLERID_SELECTOR = "[class*=lvdetails] li:contains(Seller)";
	private static final String SHIPPING_SELECTOR = "[class*=lvshipping]";
	private static final String BUYTYPE_SELECTOR = "[class*=lvformat]";
	private static final String FEEDBACKRATE_SELECTOR = "span.selrat";
	private static final String RANGEPRICE__SELECTOR = "[class*=lvprice] [class=prRange]";
	private static final String SALEPRICE__SELECTOR = "[class*=lvprice] [class=bold]";
	private static final String ORGIPRICE_SELECTOR = "[class*=lvprice] [class=stk-thr]";
	private static final String RATINGS_SELECTOR = "span.selrat";
	private static final String SOLD_SELECTOR = "[class*=hotness-signal]";
	private static final String FROMADDR_SELECTOR = "[class*=lvdetails] li:contains(From)";
	private static final String LISTITEM_SELECTOR = "#ListViewInner li[id*=item]";
	private static final String NEXT_PAGE_SELECTOR = "#Pagination .pages a.curr";
	private static final Logger log = LoggerFactory.getLogger(ListingParser.class);

	private Document doc;
	private String tmpId, url;
	private Schedule schedule;

	private LinkedList<String> img, title, sellerId, shipping, buyType, itemId, fromAddr;
	private LinkedList<Double> feedbackRate, beginPrice, endPrice, orgiPrice;
	private LinkedList<Integer> ratings, sold;
	private HttpProxy httpProxy;

	public ListingParser(Schedule schedule, String url, ProxyPool proxypool) {
		this.httpProxy = proxypool.borrow();
		this.url = url;
		this.schedule = schedule;
		this.img = new LinkedList<>();
		this.title = new LinkedList<>();
		this.sellerId = new LinkedList<>();
		this.shipping = new LinkedList<>();
		this.buyType = new LinkedList<>();
		this.itemId = new LinkedList<>();
		this.feedbackRate = new LinkedList<>();
		this.beginPrice = new LinkedList<>();
		this.endPrice = new LinkedList<>();
		this.orgiPrice = new LinkedList<>();
		this.ratings = new LinkedList<>();
		this.sold = new LinkedList<>();
		this.fromAddr = new LinkedList<>();
	}
	
	private void reset() {
		this.img.clear();
		this.title.clear();
		this.sellerId.clear();
		this.shipping.clear();
		this.buyType.clear();
		this.itemId.clear();
		this.feedbackRate.clear();
		this.beginPrice.clear();
		this.endPrice.clear();
		this.orgiPrice.clear();
		this.ratings.clear();
		this.sold.clear();
		this.fromAddr.clear();
	}

	@Override
	public void run() {
		try {
			this.doc = parseURL(url, httpProxy, null);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("parse listing url [" + url + "] error, abort", e);
		}
		
		int pgn = 1;
		while(true){
			if(doc == null ) return;
			Elements children = doc.select(LISTITEM_SELECTOR);
			for (Element element : children) {
				parseItemId(element);
				parseImg(element);
				parseTitle(element);
				parseSellerId(element);
				parseShipping(element);
				parseBuyType(element); // FBA FBM
				parseFeedbackRate(element);
				parsePrice(element);
				parseRatings(element);
				parseSold(element);
				parseFromAddr(element);
			}

			log.info("crossing [" + ("1".equals(schedule.getType()) ? "PlaceEbayByKeyWord" : ("2".equals(schedule.getType()) ? "PlaceEbayBySellerId" : "PlaceEbayByItemId")) + "] "
					+ "thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "], parse List ["+url+"] done. waiting [OfferParser: " + itemId.size() + "] Thread going on");
			// 1. save db
			insert();
	
			// 2. 10个子线程抓 销量

			int size = itemId.size() / 5;
			size = itemId.size() % 5 >= 0 ? size + 1 : size;
			for (int i = 0; i < size; i++) {
				HttpProxy proxy = DynamicIp.getAProxy();
				for (int j = 0; j < (i == size - 1 ? itemId.size() % 5 : 5); j++) {
					int _index = i * 5 + j;
					OfferPool.getInstance().execute(new OfferParser(String.format(OFFER_DETAIL_URL, schedule.getSite(), itemId.get(_index)), proxy, itemId.get(_index)));
					log.info(log.getName() + " -> index:" + _index + "," + proxy + " : offer pool: " + ThreadPoolMirror.dumpThreadPool(itemId.get(_index), OfferPool.getInstance()));
					try {
						TimeUnit.SECONDS.sleep(j+1);
					} catch (InterruptedException e) {
					}
				}
			}

			//3. Keyword只抓第一页200条，ItemId只抓第一页
			if (!"2".equals(schedule.getType())){
				break;
			}
	
			//4. 如果是根据SellerID则分页继续
			else {
				String result = parseNextPage(doc);
				if (StringUtils.isBlank(result)) {
					break;
				}
				HttpProxy httpProxy= PlaceEbayFetcher.getUSProxyHost().borrow();
				try {
					this.reset();
					url = url.replace("%26_pgn%3D" + pgn + "%26_skc%3D" + (pgn * 200 - 200), "%26_pgn%3D" + (pgn + 1) + "%26_skc%3D" + ((pgn + 1) * 200 - 200));
					this.doc = parseURL(url, httpProxy, null);
					pgn++;
				} catch (Exception e) {
					e.printStackTrace();
					log.error("parse listing url [" + url + "] error, abort", e);
				} finally {
				}
			}
		}
		log.info("finished [" + ("1".equals(schedule.getType()) ? "PlaceEbayByKeyWord" : ("2".equals(schedule.getType()) ? "PlaceEbayBySellerId" : "PlaceEbayByItemId")) + "] "
				+ "thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "]. ");
	}

	private void parseItemId(Element element) {
		tmpId = element.attr("listingid");
		this.itemId.add(tmpId);
	}

	private void parseImg(Element element) {
		try {
			Elements img = element.select(IMG_SELECTOR);
			if (img.size() == 0)
				throw new IllegalAccessException();
			this.img.add(img.attr("src"));
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [img]", e);
			this.img.add(null);
		}
	}

	private void parseTitle(Element element) {
		try {
			Elements title = element.select(TITLE_SELECTOR);
			if (title.size() == 0)
				throw new IllegalAccessException();
			this.title.add(title.text());
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [title]", e);
			this.title.add(null);
		}
	}

	private void parseSellerId(Element element) {
		try {
			if (schedule != null && "2".equals(schedule.getType())) {
				this.sellerId.add(schedule.getSearchTerm());
				return;
			}
			Elements li = element.select(SELLERID_SELECTOR);
			if (li.size() == 0)
				throw new IllegalAccessException();
			Elements sellerId = li.select("a");
			if (sellerId.size() == 0) {
				this.sellerId.add(null);
			} else {
				String tmp = sellerId.attr("href");
				if (StringUtils.isNotBlank(tmp))
					tmp = tmp.substring((tmp.lastIndexOf("/") > -1 ? tmp.lastIndexOf("/") + 1 : 0));
				this.sellerId.add(tmp);
			}
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [sellerId]", e);
			this.sellerId.add(null);
		}
	}

	private void parseShipping(Element element) {
		try {
			Elements shipping = element.select(SHIPPING_SELECTOR);
			if (shipping.size() == 0)
				throw new IllegalAccessException();
			this.shipping.add(shipping.text());
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [shipping]", e);
			this.shipping.add(null);
		}
	}

	private void parseBuyType(Element element) {
		try {
			Elements buyType = element.select(BUYTYPE_SELECTOR);
			if (buyType.size() == 0)
				throw new IllegalAccessException();
			this.buyType.add(buyType.last().text());
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [buyType]", e);
			this.buyType.add(null);
		}
	}

	private void parseFeedbackRate(Element element) {
		try {
			Elements feedbackRate = element.select(SELLERID_SELECTOR).select(FEEDBACKRATE_SELECTOR);
			if (feedbackRate.size() == 0)
				throw new IllegalAccessException();
			String tmp = feedbackRate.last().text().replaceAll("[^\\d.]", "");
			if (StringUtils.isNotBlank(tmp))
				this.feedbackRate.add(Double.valueOf(tmp));
			else
				this.feedbackRate.add(null);
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [feedbackRate]", e);
			this.feedbackRate.add(null);
		}
	}

	private void parsePrice(Element element) {
		try {
			Elements rangePrice = element.select(RANGEPRICE__SELECTOR);
			if (rangePrice.size() > 0 && StringUtils.isNotBlank(rangePrice.text())) {
				String[] prcs = rangePrice.text().split("to");
				if (prcs.length == 1) {
					this.beginPrice.add(StringUtils.isNotBlank(prcs[0]) ? Double.valueOf(prcs[0].replaceAll("[^\\d.]", "")) : null);
					this.endPrice.add(null);
				}
				if (prcs.length == 2) {
					this.beginPrice.add(StringUtils.isNotBlank(prcs[0]) ? Double.valueOf(prcs[0].replaceAll("[^\\d.]", "")) : null);
					this.endPrice.add(StringUtils.isNotBlank(prcs[1]) ? Double.valueOf(prcs[1].replaceAll("[^\\d.]", "")) : null);
				}

				Elements orgiPrice = element.select(ORGIPRICE_SELECTOR);
				if (orgiPrice.size() > 0 && StringUtils.isNotBlank(orgiPrice.text())) {
					String tmp = orgiPrice.text().replaceAll("[^\\d.]", "");
					this.orgiPrice.add(Double.valueOf(tmp));
				} else {
					this.orgiPrice.add(null);
				}
			} else {
				Elements salePrice = element.select(SALEPRICE__SELECTOR);
				if (salePrice.size() > 0) {
					String tmp = salePrice.last().text();
					/*if (tmp.indexOf(salePrice.last().children().text()) > -1)
						tmp = tmp.substring(0, tmp.indexOf(salePrice.last().children().text()));
					tmp = tmp.replaceAll("[^\\d.]", "");*/
					Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
					Matcher matcher = pattern.matcher(tmp);
					if (matcher.find())
						tmp = matcher.group(1);
					if (StringUtils.isNotBlank(tmp)) {
						this.beginPrice.add(Double.valueOf(tmp));
						this.endPrice.add(null);
						Elements orgiPrice = element.select(ORGIPRICE_SELECTOR);
						if (orgiPrice.size() > 0 && StringUtils.isNotBlank(orgiPrice.text())) {
							String tmp1 = orgiPrice.text().replaceAll("[^\\d.]", "");
							this.orgiPrice.add(Double.valueOf(tmp1));
						} else {
							this.orgiPrice.add(null);
						}
					} else {
						this.beginPrice.add(null);
						this.endPrice.add(null);
						Elements orgiPrice = element.select(ORGIPRICE_SELECTOR);
						if (orgiPrice.size() > 0 && StringUtils.isNotBlank(orgiPrice.text())) {
							String tmp1 = orgiPrice.text().replaceAll("[^\\d.]", "");
							this.orgiPrice.add(Double.valueOf(tmp1));
						} else {
							this.orgiPrice.add(null);
						}
					}
				} else {
					this.beginPrice.add(null);
					this.endPrice.add(null);
					Elements orgiPrice = element.select(ORGIPRICE_SELECTOR);
					if (orgiPrice.size() > 0 && StringUtils.isNotBlank(orgiPrice.text())) {
						String tmp = orgiPrice.text().replaceAll("[^\\d.]", "");
						this.orgiPrice.add(Double.valueOf(tmp));
					} else {
						this.orgiPrice.add(null);
					}
				}
			}
			// String etprice =
			// StringUtil.isBlank(e.text().replaceAll("[$,£,EUR,CDN$,?]", "")) ?
			// "0" : e.text().replaceAll("[$,£,EUR,CDN$,?]", "");
			// //$美国，澳大利亚，//EUR,法国，德国，£,英国，CDN$,加拿大
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [price]", e);
			this.beginPrice.add(null);
			this.endPrice.add(null);
			this.orgiPrice.add(null);
		}
	}

	private void parseRatings(Element element) {
		try {
			Elements ratings = element.select(SELLERID_SELECTOR).select(RATINGS_SELECTOR);
			if (ratings.size() == 0)
				throw new IllegalAccessException();
			String tmp = ratings.first().text().replaceAll("[^\\d.]", "");
			if (StringUtils.isNotBlank(tmp))
				this.ratings.add(Integer.valueOf(tmp));
			else
				this.ratings.add(0);
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [ratings]", e);
			this.ratings.add(0);
		}
	}

	private void parseSold(Element element) {
		try {
			Elements sold = element.select(SOLD_SELECTOR);
			if (sold.size() == 0)
				throw new IllegalAccessException();
			if (StringUtils.isNotBlank(sold.first().text()) && sold.first().text().contains("sold")) {
				String tmp = sold.first().text().replaceAll("[^\\d.]", "");
				if (StringUtils.isNotBlank(tmp))
					this.sold.add(Integer.valueOf(tmp));
				else
					this.sold.add(0);
			} else {
				this.sold.add(0);
			}
		} catch (Exception e) {
			this.sold.add(0);
		}
	}

	/**
	 * vpn有可能取不到，因此可空
	 * @param element
	 */
	private void parseFromAddr(Element element) {
		try {
			Elements fromAddr = element.select(FROMADDR_SELECTOR);
			if (fromAddr.size() == 0)
				throw new IllegalAccessException();
			if (fromAddr.first() != null && StringUtils.isNotBlank(fromAddr.first().text())) {
				if (fromAddr.first().text().contains("From "))
					this.fromAddr.add(fromAddr.first().text().substring(5));
				else
					this.fromAddr.add(fromAddr.first().text());
			} else {
				this.fromAddr.add(null);
			}
		} catch (Exception e) {
//			log.error("itemId: [" + tmpId + "] error, cannot get [fromAddr]", e);
			this.fromAddr.add(null);
		}
	}
	
	private String parseNextPage(Document doc) {
		Elements currentpage = doc.select(NEXT_PAGE_SELECTOR);
		if(currentpage.size() == 0)
			return null;
		Element nextpage = currentpage.first().nextElementSibling();
		if(nextpage == null)
			return null;
		String nexthref = nextpage.attr("href");
		if(StringUtils.isNotBlank(nexthref))
			return nexthref;
		return null;
	}

	// save to db
	void insert() {
		String sql = "insert into t_listing (schedule_id, site, item_id, title, seller_id, shipping, buy_type, from_addr, feedback_rate, begin_price, end_price, org_price, ratings, sold, create_by) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
		Connection conn = null;
		try {
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = itemId.size() / 100;
			size = itemId.size() % 100 >= 0 ? size + 1 : size; // 5521,5
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? itemId.size() % 100 : 100); j++) {
					int _index = i * 100 + j;
					pstmt.setString(1, this.schedule.getId());
					pstmt.setString(2, this.schedule.getSite());
					pstmt.setString(3, this.itemId.get(_index));
					pstmt.setString(4, this.title.get(_index));
					pstmt.setString(5, this.sellerId.get(_index));
					pstmt.setString(6, this.shipping.get(_index));
					pstmt.setString(7, this.buyType.get(_index));
					pstmt.setString(8, this.fromAddr.get(_index));
					if (this.feedbackRate.get(_index) == null)
						pstmt.setNull(9, Types.DOUBLE);
					else
						pstmt.setDouble(9, this.feedbackRate.get(_index));

					if (this.beginPrice.get(_index) == null)
						pstmt.setNull(10, Types.DOUBLE);
					else
						pstmt.setDouble(10, this.beginPrice.get(_index));

					if (this.endPrice.get(_index) == null)
						pstmt.setNull(11, Types.DOUBLE);
					else
						pstmt.setDouble(11, this.endPrice.get(_index));

					if (this.orgiPrice.get(_index) == null)
						pstmt.setNull(12, Types.DOUBLE);
					else
						pstmt.setDouble(12, this.orgiPrice.get(_index));

					if (this.ratings.get(_index) == null)
						pstmt.setNull(13, Types.INTEGER);
					else
						pstmt.setInt(13, this.ratings.get(_index));

					if (this.sold.get(_index) == null)
						pstmt.setNull(14, Types.INTEGER);
					else
						pstmt.setInt(14, this.sold.get(_index));
					pstmt.setString(15, schedule.getCreate_by());
					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.clearBatch();
			}
			conn.commit();
			pstmt.close();
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
	}

}
