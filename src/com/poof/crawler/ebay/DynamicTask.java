package com.poof.crawler.ebay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

import com.poof.crawler.db.entity.Schedule;
import com.poof.crawler.utils.pool.KeyWordPool;
import com.poof.crawler.utils.pool.SellerIDPool;

/**
 * @author wilkey
 * @mail admin@wilkey.vip
 * @Date 2017年1月17日 下午2:26:22
 */
public class DynamicTask {

	private static Logger log = Logger.getLogger(DynamicTask.class);

	private ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application.xml");
	private Scheduler schedulerFactory = (Scheduler) context.getBean("schedulerFactory");

	/**
	 * 更新定时任务的触发表达式
	 * 
	 * @param triggerName
	 *            触发器名字
	 * @param start
	 *            触发表达式
	 * @return 成功则返回true，否则返回false
	 */
	public boolean startOrStop(String triggerName, boolean start) {
		try {
			CronTrigger trigger = (CronTrigger) getTrigger(triggerName, Scheduler.DEFAULT_GROUP);
			if (start) {
				schedulerFactory.resumeTrigger(trigger.getName(), trigger.getGroup());
				log.info("trigger the start successfully!!");
			} else {
				schedulerFactory.pauseTrigger(trigger.getName(), trigger.getGroup());
				log.info("trigger the pause successfully!!");
			}
			return true;
		} catch (SchedulerException e) {
			log.error("Fail to reschedule. " + e);
			return false;
		}
	}

	/**
	 * 更新定时任务的触发表达式
	 * 
	 * @param triggerName
	 *            触发器名字
	 * @param cronExpression
	 *            触发表达式
	 * @return 成功则返回true，否则返回false
	 */
	public boolean updateCronExpression(String triggerName, String cronExpression) {
		try {
			CronTrigger trigger = (CronTrigger) getTrigger(triggerName, Scheduler.DEFAULT_GROUP);
			if (trigger == null) {
				return false;
			}
			if (StringUtils.equals(trigger.getCronExpression(), cronExpression)) {
				log.info("cronExpression is same with the running Schedule , no need to update.");
				return true;
			}
			trigger.setCronExpression(cronExpression);
			schedulerFactory.rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
			// updateSpringMvcTaskXML(trigger, cronExpression);
			log.info("Update the cronExpression successfully!!");
			return true;
		} catch (ParseException e) {
			log.error("The new cronExpression - " + cronExpression + " not conform to the standard. " + e);
			return false;
		} catch (SchedulerException e) {
			log.error("Fail to reschedule. " + e);
			return false;
		}
	}

	/**
	 * 获取触发器
	 * 
	 * @param triggerName
	 *            触发器名字
	 * @param groupName
	 *            触发器组名字
	 * @return 对应Trigger
	 */
	private Trigger getTrigger(String triggerName, String groupName) {
		Trigger trigger = null;
		if (StringUtils.isBlank(groupName)) {
			return null;
		}
		if (StringUtils.isBlank(triggerName)) {
			return null;
		}
		try {
			trigger = schedulerFactory.getTrigger(triggerName, groupName);
		} catch (SchedulerException e) {
			return null;
		}
		return trigger;
	}

	/**
	 * 更新application-timer.xml 配置文件
	 * 
	 * @param trigger
	 * @param cronExpression
	 */
	@SuppressWarnings("unchecked")
	public synchronized static void updateSpringMvcTaskXML(CronTrigger trigger, String cronExpression) {
		Document document = null;
		File file = null;
		SAXReader saxReader = new SAXReader();
		try {
			URI url = DynamicTask.class.getClassLoader().getResource("application-timer.xml").toURI();
			file = new File(url.getPath());
			document = saxReader.read(new FileInputStream(file));
		} catch (Exception e) {
			throw new RuntimeException("---------读取application-timer.xml文件出错:" + e.getMessage());
		}
		Element root = document.getRootElement();
		List<Element> beans = root.elements();
		for (Element bean : beans) {
			if (bean.attribute("id") != null && bean.attribute("id").getValue().equals(trigger.getName())) {
				beans = bean.elements();
				for (Element temp : beans) {
					if (temp.attribute("name") != null && temp.attribute("name").getValue().equals("cronExpression")) {
						temp.attribute("value").setValue(cronExpression);
						break;
					}
				}
				break;
			}
		}
		XMLWriter fileWriter = null;
		try {
			OutputFormat xmlFormat = OutputFormat.createPrettyPrint();
			xmlFormat.setEncoding("utf-8");
			fileWriter = new XMLWriter(new FileOutputStream(file), xmlFormat);
			fileWriter.write(document);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 动态添加后执行
	 * 
	 * @param ebayQtRule
	 * @param start
	 * @throws Exception
	 */
	public void dynamicExec(List<Schedule> list, boolean start) throws Exception {
		for (int i = 0; i < list.size(); i++) {
			CronTrigger trigger = (CronTrigger) schedulerFactory.getTrigger(list.get(i).getName() + list.get(i).getId(), Scheduler.DEFAULT_GROUP);
			if (null == trigger) {// 如果不存在该trigger则创建一个
				MethodInvokingJobDetailFactoryBean jobDetail = new MethodInvokingJobDetailFactoryBean();
				jobDetail.setConcurrent(false);
				jobDetail.setTargetMethod("execute");
				jobDetail.setName(list.get(i).getName() + list.get(i).getId());

				if ("1".equals(list.get(i).getType())) {
					jobDetail.setTargetObject(KeyWordPool.getInstance());
					jobDetail.setArguments(new Object[] { new PlaceEbayByKeyWordFetcher(list.get(i)) });
				} else if ("2".equals(list.get(i).getType())) {
					jobDetail.setTargetObject(SellerIDPool.getInstance());
					jobDetail.setArguments(new Object[] { new PlaceEbayBySellerIdFetcher(list.get(i)) });
				}
				jobDetail.afterPropertiesSet();

				trigger = new CronTrigger(list.get(i).getName() + list.get(i).getId(), Scheduler.DEFAULT_GROUP, list.get(i).getCronexp());
				schedulerFactory.scheduleJob((JobDetail) jobDetail.getObject(), trigger);
				log.info("new schedule name: [" + (list.get(i).getName() + list.get(i).getId()) + "] runned...");
			} else if (null != trigger) {
				// Trigger已存在，那么更新相应的定时设置
				if ("1".equals(list.get(i).getStatus())) {
					trigger.setCronExpression(list.get(i).getCronexp());
					schedulerFactory.rescheduleJob(trigger.getName(), trigger.getGroup(), trigger);
					log.info("exists schedule name: [" + (list.get(i).getName() + list.get(i).getId()) + "] refreshed...");
				} else {
					remove(list.get(i).getName() + list.get(i).getId());
					log.info("invaild schedule name: [" + (list.get(i).getName() + list.get(i).getId()) + "] deleted...");
				}
			}
			schedulerFactory.triggerJob(list.get(i).getName() + list.get(i).getId(), Scheduler.DEFAULT_GROUP);
			if (trigger != null)
				this.startOrStop(trigger.getName(), start);
		}
	}

	/**
	 * 删除任务
	 * 
	 * @param list
	 * @throws SchedulerException
	 */
	public void remove(String triggerName) throws SchedulerException {

		CronTrigger trigger = (CronTrigger) getTrigger(triggerName, Scheduler.DEFAULT_GROUP);
		if (trigger != null) {
			schedulerFactory.pauseTrigger(trigger.getName(), Scheduler.DEFAULT_GROUP);// 停止触发器
			schedulerFactory.unscheduleJob(trigger.getName(), Scheduler.DEFAULT_GROUP);// 移除触发器
			schedulerFactory.deleteJob(trigger.getName(), Scheduler.DEFAULT_GROUP);// 删除任务
		}
	}

}
