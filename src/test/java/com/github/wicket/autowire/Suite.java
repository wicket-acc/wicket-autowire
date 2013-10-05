package com.github.wicket.autowire;

import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

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
	public void testSubClassPage() {
		tester.startPage(SubClassPage.class);
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
