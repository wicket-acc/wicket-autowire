package com.github.wicket.autowire;

import org.apache.wicket.util.tester.WicketTester;

import com.github.wicket.autowire.AutoWire;

public class AutoWireTester extends WicketTester {

	public AutoWireTester() {
		getApplication().getComponentInitializationListeners().add(new AutoWire());
	}

}