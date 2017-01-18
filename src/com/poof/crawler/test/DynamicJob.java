package com.poof.crawler.test;

import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.quartz.CronTriggerBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

import com.poof.crawler.ebay.App;
import com.poof.crawler.test.EbayFetcher;

public class DynamicJob {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ClassPathResource res = new ClassPathResource("application.xml");
		XmlBeanFactory factory = new XmlBeanFactory(res);

		Scheduler scheduler = (Scheduler) factory.getBean("schedulerFactory");

		App a = new App();

		try {
			// create JOB
			MethodInvokingJobDetailFactoryBean jobDetail = new MethodInvokingJobDetailFactoryBean();
			jobDetail.setTargetObject(a);
			jobDetail.setTargetMethod("DynamicTask");
			jobDetail.setName("ebayJob");
			jobDetail.setConcurrent(false);
			jobDetail.afterPropertiesSet();

			CronTriggerBean cronTrigger = new CronTriggerBean();
			cronTrigger.setBeanName("ebaycron");

			String expression = "0/1 * * * * ?";
			cronTrigger.setCronExpression(expression);
			cronTrigger.afterPropertiesSet();

			scheduler.scheduleJob((JobDetail) jobDetail.getObject(), cronTrigger);

			scheduler.start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}