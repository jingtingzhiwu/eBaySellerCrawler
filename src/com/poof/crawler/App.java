package com.poof.crawler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.poof.crawler.db.DBUtil;
import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.ebay.DynamicTask;
import com.poof.crawler.proxy.DynamicIp;

/**
 * @author wilkey
 * @desc 每日0点更新schedule, 动态刷新
 * @mail admin@wilkey.vip
 * @Date 2017年1月18日 上午10:47:33
 */
@Component
public class App {

	private static Logger log = Logger.getLogger(App.class);
	private static Scheduler schedulerFactory;

	@Scheduled(cron = "0 0 0 * * *")
	public void DynamicTask() throws Exception {
		List<Schedule> list = getScheduleFromDB();
		if (list.isEmpty()) {
			log.info("starting DynamicTask to crawl eBay, no schedule and return");
			return;
		}
		DynamicTask dynamicTask = new DynamicTask(schedulerFactory);

		dynamicTask.dynamicExec(list, true);
	}

	public static void main(String[] args) throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
		schedulerFactory = (Scheduler) context.getBean("scheduler");
		DruidDataSource dataSource = (DruidDataSource)context.getBean("dataSource");
		DBUtil.setDataSource(dataSource);
		context.start();
		App app = new App();
		app.DynamicTask();
		new Thread(new DynamicIp()).start();
		System.err.println("starting......");
		System.in.read();
	}

	public List<Schedule> getScheduleFromDB() {
		try {
			List<Schedule> result = new ArrayList<Schedule>();
			List<Schedule> vaild = DBUtil.queryBeanList(DBUtil.openConnection(), "select * from t_schedule where status = 1 group by search_term,site,type order by type desc", Schedule.class);
			List<Schedule> invaild = DBUtil.queryBeanList(DBUtil.openConnection(), "select * from t_schedule where status = 0 ", Schedule.class);
			log.info("refreshing DynamicTask to crawl eBay, activity schedule: [" + (vaild != null ? vaild.size() : 0) + "], deleted schedule: [" + (invaild != null ? invaild.size() : 0) + "]");
			if (!vaild.isEmpty())
				result.addAll(vaild);
			if (!invaild.isEmpty())
				result.addAll(invaild);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return new ArrayList<Schedule>();
	}
}
