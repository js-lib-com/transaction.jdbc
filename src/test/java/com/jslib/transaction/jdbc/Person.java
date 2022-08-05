package com.jslib.transaction.jdbc;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class Person {
	public int id = 1;
	public String name = "John";
	public String surname = "Doe";
	public URL webPage = URL("http://site.com/");
	public String landline = "+40 232 555-666";
	public String mobile = "+40 721 333-444";
	public String emailAddr = "john.doe@email.com";
	public Date birthday = date("1964-03-15 13:40:00");
	public State state = State.NONE;

	static Person instance() {
		return new Person();
	}

	static Person instance(int id) {
		Person p = new Person();
		p.id = id;
		return p;
	}

	static List<Person> list() {
		List<Person> list = new ArrayList<Person>();
		list.add(instance());
		list.add(instance());
		list.add(instance());
		return list;
	}

	private static URL URL(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
		}
		return null;
	}

	private static Date date(String date) {
		try {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
		} catch (ParseException e) {
		}
		return null;
	}
}
