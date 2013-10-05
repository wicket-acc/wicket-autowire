package com.github.wicket.autowire;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;

import com.github.wicket.autowire.Component;


public class TestBorder extends Border {

	private static final long serialVersionUID = 1L;

	@Component
	Label borderLabel;

	public TestBorder(final String id) {
		super(id);
	}

}
