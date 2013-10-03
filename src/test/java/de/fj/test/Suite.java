package de.fj.test;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import de.fj.test.autowire.BasicPage;
import de.fj.test.autowire.BasicPanel;
import de.fj.test.autowire.BorderPage;
import de.fj.test.autowire.ContainerPage;

public class Suite {

	private WicketTester tester;

	@Before
	public void setUp() {
		tester = new AutoWireTester();
	}

	@Test
	public void testBasicPage() {
		tester.startPage(BasicPage.class);
	}

	@Test
	public void testBasicPanel() {
		tester.startComponentInPage(BasicPanel.class);
	}

	@Test
	public void testContainer() {
		tester.startPage(ContainerPage.class);
	}

	@Test
	public void testBorder() {
		tester.startPage(BorderPage.class);
	}
}
