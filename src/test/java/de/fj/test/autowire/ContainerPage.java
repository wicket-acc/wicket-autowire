package de.fj.test.autowire;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import de.fj.test.Component;

public class ContainerPage extends WebPage {

	private static final long serialVersionUID = 1L;

	@Component
	WebMarkupContainer container;

	@Component
	Label label;

}
