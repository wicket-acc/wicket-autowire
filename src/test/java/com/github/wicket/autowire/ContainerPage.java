package com.github.wicket.autowire;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import com.github.wicket.autowire.Component;


public class ContainerPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Component
	WebMarkupContainer container;

	@Component
	Label label;

}
