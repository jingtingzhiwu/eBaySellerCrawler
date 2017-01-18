package com.poof.crawler.db.entity;

public class Schedule {
	private String id;
	private String name;
	private String cronexp;
	private String type;
	private String site;
	private String search_term;
	private int status;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCronexp() {
		return cronexp;
	}

	public void setCronexp(String cronexp) {
		this.cronexp = cronexp;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getSearchTerm() {
		return search_term;
	}

	public void setSearchTerm(String searchTerm) {
		this.search_term = searchTerm;
	}
}
