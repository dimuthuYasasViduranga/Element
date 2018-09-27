package com.element.analytics.models;

public class User {

	private String fname;
	private String lname;
	private String email;
	private String contactNo;
	private String domain;
	private String password;
	public User(String fname, String lname, String email, String contactNo, String domain, String password) {
		super();
		this.fname = fname;
		this.lname = lname;
		this.email = email;
		this.contactNo = contactNo;
		this.domain = domain;
		this.password = password;
	}
	public String getFname() {
		return fname;
	}
	public void setFname(String fname) {
		this.fname = fname;
	}
	public String getLname() {
		return lname;
	}
	public void setLname(String lname) {
		this.lname = lname;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getContactNo() {
		return contactNo;
	}
	public void setContactNo(String contactNo) {
		this.contactNo = contactNo;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
