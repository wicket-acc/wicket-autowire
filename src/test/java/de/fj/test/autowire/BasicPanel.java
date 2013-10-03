package de.fj.test.autowire;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

import de.fj.test.Component;

public class BasicPanel extends Panel {

	private static final long serialVersionUID = 1L;

	public BasicPanel(final String id) {
		super(id);
	}

	@Component
	TestLink link;

	@Component
	Label label;

	class TestLink extends Link<Object> {

		private static final long serialVersionUID = 1L;

		public TestLink(final String id) {
			super(id);
		}

		@Override
		public void onClick() {

		}

	}

}
