package de.fj.test;

import org.apache.wicket.util.tester.WicketTester;

public class AutoWireTester extends WicketTester {

	public AutoWireTester() {
		getApplication().getComponentInitializationListeners().add(new AutoWire());
	}

}