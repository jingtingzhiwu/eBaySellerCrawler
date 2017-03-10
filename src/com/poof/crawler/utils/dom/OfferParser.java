package com.poof.crawler.utils.dom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.poof.crawler.db.DBUtil;
import com.poof.crawler.proxy.HttpProxy;

public class OfferParser extends Parser implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(OfferParser.class);
	private static final String REFER_SELECTOR = "[class=statusMessage]";
	private static final String PRICE_SELECTOR = "[class*=priceData]";
	private static final String SOLD_SELECTOR = "td:contains(shows the last 100 transactions)";
	private static final String SOLD_SPECIAL = "Sold as a special offer";
	// private static final SimpleDateFormat sdf = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat ensdf = new SimpleDateFormat("MMM-d-yy HH:mm:ss", Locale.US);

	private Document doc;
	private String itemId;
	private Integer sold;

	private ArrayList<String> userId, purchaseDate, variation, isOffer;
	private ArrayList<Double> price;
	private ArrayList<Integer> quantity;
	private HttpProxy httpProxy;
	private String url;

	public OfferParser(String url, HttpProxy httpProxy, String itemId) {
		this.itemId = itemId;
		this.url = url;
		this.httpProxy = httpProxy;
		this.userId = new ArrayList<>();
		this.price = new ArrayList<>();
		this.quantity = new ArrayList<>();
		this.purchaseDate = new ArrayList<>();
		this.variation = new ArrayList<>();
		this.isOffer = new ArrayList<>();
	}

	private void reset() {
		this.userId.clear();
		this.price.clear();
		this.quantity.clear();
		this.purchaseDate.clear();
		this.variation.clear();
		this.isOffer.clear();
	}

	/**
	 * REFER_SELECTOR 会有多个，如112051614873，取第一个，其他可忽略
	 */
	@Override
	public void run() {
		try {
			this.doc = parseURL(url, httpProxy, null);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		}

		if (doc == null)
			return;
		Elements refer = doc.select(REFER_SELECTOR);
		if (refer.size() > 0) {
			for (Element element : refer) {
				reset();
				if (element == null || element.siblingElements().size() == 0)
					return;
				if (element.siblingElements().select("table").size() == 0 || element.siblingElements().select("table").last() == null)
					return;
				Element tableElement = element.siblingElements().select("table").last();
				Elements trs = tableElement.select("tr:gt(0)");
				if (trs.size() == 0)
					return;

				parseOfferHistory(trs);

				insert();

				update();
				log.info(log.getName() + " : offer parse done: " + itemId);
			}
		}
		else
			log.info(log.getName() + " : None offer parse: " + itemId);
	}

	/**
	 * 
	 * @param price
	 *            Sold as a special offer
	 */
	private void parseOfferHistory(Elements trs) {
		for (Element element : trs) {
			try {
				if (element != null) {
					Elements tds = element.select("td");
					if (tds.size() == 7) { // 7是多属性
						// tds.get(0) 是ebay留出来的空白单元格
						this.userId.add(StringUtils.isNotBlank(tds.get(1).text()) ? tds.get(1).text() : null);
						this.variation.add(StringUtils.isNotBlank(tds.get(2).text()) ? tds.get(2).text() : null);
						this.price.add(StringUtils.isNotBlank(tds.get(3).text()) && StringUtils.isNotBlank(tds.get(3).text().replaceAll("[^\\d.]", ""))
								? Double.valueOf(tds.get(3).text().replaceAll("[^\\d.]", "")) : null);
						this.quantity.add(StringUtils.isNotBlank(tds.get(4).text()) && StringUtils.isNotBlank(tds.get(4).text().replaceAll("[^\\d]", ""))
								? Integer.valueOf(tds.get(4).text().replaceAll("[^\\d]", "")) : null);
						this.purchaseDate.add(StringUtils.isNotBlank(tds.get(5).text()) ? tds.get(5).text() : null);
						this.isOffer.add("0");
					} else if (tds.size() == 6) { // 6是单属性或offerHistory
						String _currentprice = doc.select(PRICE_SELECTOR).first().text().replaceAll("[^\\d.]", "");
						if (tds.get(2).text().equalsIgnoreCase(ACCEPTED)) {
							this.price.add(StringUtils.isNotBlank(_currentprice) ? Double.valueOf(_currentprice) : null);
							this.isOffer.add("1");
						} else if (tds.get(2).text().equalsIgnoreCase(DECLINED) || tds.get(2).text().equalsIgnoreCase(EXPIRED) || tds.get(2).text().equalsIgnoreCase(PENDING)) {
							continue;
						} else {
							if (tds.get(2).text().equalsIgnoreCase(SOLD_SPECIAL)) {
								this.price.add(Double.valueOf(_currentprice));
								this.isOffer.add("1");
							} else {
								this.price.add(StringUtils.isNotBlank(tds.get(2).text()) ? Double.valueOf(tds.get(2).text().replaceAll("[^\\d.]", "")) : null);
								this.isOffer.add("0");
							}
						}
						this.userId.add(StringUtils.isNotBlank(tds.get(1).text()) ? tds.get(1).text() : null);
						this.variation.add(null);
						this.quantity.add(StringUtils.isNotBlank(tds.get(3).text()) && StringUtils.isNotBlank(tds.get(3).text().replaceAll("[^\\d]", ""))
								? Integer.valueOf(tds.get(3).text().replaceAll("[^\\d]", "")) : null);
						this.purchaseDate.add(StringUtils.isNotBlank(tds.get(4).text()) ? tds.get(4).text() : null);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error("itemId: [" + itemId + "] parse error, Html : \n" + element.html(), e);
			}
		}
		if (trs.select(SOLD_SELECTOR).size() > 0) {
			String tmp = trs.select("td:contains(shows the last 100 transactions)").last().text();
			tmp = tmp.substring(tmp.indexOf("transactions")).replaceAll("[^\\d]", "");
			this.sold = StringUtils.isNotBlank(tmp) ? Integer.valueOf(tmp) : null;
		} else {
			this.sold = trs.size();
		}
	}

	@Override
	void insert() {
		String sql = "insert into t_listing_offer (item_id, user_id, variation, price, quantity, purchase_date, is_offer) values (?,?,?,?,?,?,?);";
		Connection conn = null;
		try {
			conn = DBUtil.openConnection();
			conn.setAutoCommit(false);
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			int size = userId.size() / 100;
			size = userId.size() % 100 >= 0 ? size + 1 : size; // 5521,5
			for (int i = 0; i < size; i++) { // 6
				for (int j = 0; j < (i == size - 1 ? userId.size() % 100 : 100); j++) {
					int _index = i * 100 + j;
					pstmt.setString(1, this.itemId);
					pstmt.setString(2, this.userId.get(_index));
					if (this.variation.get(_index) == null)
						pstmt.setNull(3, Types.VARCHAR);
					else
						pstmt.setString(3, this.variation.get(_index));
					if (this.price.get(_index) == null)
						pstmt.setNull(4, Types.INTEGER);
					else
						pstmt.setDouble(4, this.price.get(_index));
					if (this.quantity.get(_index) == null)
						pstmt.setNull(5, Types.DOUBLE);
					else
						pstmt.setInt(5, this.quantity.get(_index));
					pstmt.setTimestamp(6, new Timestamp(ensdf.parse(this.purchaseDate.get(_index)).getTime()));
					pstmt.setString(7, this.isOffer.get(_index));
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
		}
	}

	void update() {
		try {
			String sql = "update t_listing set sold = " + this.sold + " where item_id=" + this.itemId + " and  id in (select id  from (select max(id) as id from t_listing where item_id=" + this.itemId
					+ " ) tmp )";
			DBUtil.execute(DBUtil.openConnection(), sql);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(log.getName() + " : program error: " + e);
		} finally {
		}
	}
}
