package com.poof.crawler.utils.dom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedList;
import java.util.Random;
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
import com.poof.crawler.db.entity.ProxyHost;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.pool.OfferPool;

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
	private ProxyHost proxy;
	private Schedule schedule;

	private LinkedList<String> img, title, sellerId, shipping, buyType, itemId, fromAddr;
	private LinkedList<Double> feedbackRate, beginPrice, endPrice, orgiPrice;
	private LinkedList<Integer> ratings, sold;

	public ListingParser(Schedule schedule, String url, ProxyHost proxy) {
		try {
			this.url = url;
			this.doc = parseURL(url, proxy, null);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("parse listing url [" + url + "] error, abort", e);
		}

		this.schedule = schedule;
		this.proxy = proxy;
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

	@Override
	public synchronized void run() {
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

			log.info("crossing [" + ("1".equals(schedule.getType()) ? "PlaceEbayByKeyWord" : ("2".equals(schedule.getType()) ? "PlaceEbayBySellerId" : "Other")) + "] "
					+ "thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "], parse List ["+url+"] done. waiting [OfferParser] Thread going on");
			try {
				TimeUnit.SECONDS.sleep(new Random().nextInt(20));
			} catch (InterruptedException e) {
				e.printStackTrace();
				log.error(log.getName() + " : program error: " + e);
			}
			// 1. save db
			insert();
	
			// 2. 10个子线程抓 销量
			for (int i = 0; i < itemId.size(); i++) {
				OfferPool.getInstance().execute(new OfferParser(String.format(OFFER_DETAIL_URL, schedule.getSite(), itemId.get(i)), this.proxy, itemId.get(i)));
			}

			//3. Keyword只抓第一页200条
			if("1".equals(schedule.getType()))
				break;
	
			//4. 如果是根据SellerID则分页继续
			if("2".equals(schedule.getType())){
				url = parseNextPage(doc);
				if (StringUtils.isBlank(url)) {
					break;
				}
				try {
					this.doc = parseURL(url, proxy, null);
				} catch (Exception e) {
					e.printStackTrace();
					log.error("parse listing url [" + url + "] error, abort", e);
				}
			}
		}
		log.info("finished [PlaceEbayByKeyWord] thread name: [" + schedule.getName() + "], site: [" + schedule.getSite() + "], searchterm: [" + schedule.getSearchTerm() + "], parse List done. waiting [OfferParser] Thread going on");
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
			log.error("itemId: [" + tmpId + "] error, cannot get [img]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [title]" + "\n" + element.html(), e);
			this.title.add(null);
		}
	}

	private void parseSellerId(Element element) {
		try {
			Elements sellerId = element.select(SELLERID_SELECTOR);
			if (sellerId.size() == 0)
				throw new IllegalAccessException();
			this.sellerId.add(sellerId.text().substring(sellerId.text().indexOf(": ") + 2, sellerId.text().indexOf("(")));
		} catch (Exception e) {
			log.error("itemId: [" + tmpId + "] error, cannot get [sellerId]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [shipping]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [buyType]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [feedbackRate]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [price]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [ratings]" + "\n" + element.html(), e);
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
			log.error("itemId: [" + tmpId + "] error, cannot get [fromAddr]" + "\n" + element.html(), e);
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
		String sql = "insert into t_listing (schedule_id, site, item_id, title, seller_id, shipping, buy_type, from_addr, feedback_rate, begin_price, end_price, org_price, ratings, sold) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
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
