package com.github.wicket.autowire;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import com.github.wicket.autowire.Component;


public class BorderPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Component
	TestBorder border;

	@Component
	Label label1;

	@Component
	Label label2;

}
