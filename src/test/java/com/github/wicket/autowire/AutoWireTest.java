/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.github.wicket.autowire;

import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoWireTest {

  private AutoWireTester tester;

  @Before
  public void setUp() {
    this.tester = new AutoWireTester();
  }

  @Test
  public void testBasicPage() {
    this.tester.startPage(BasicPage.class);
  }

  /**
   * Assert that instantiation of inner classes works and that components are
   * added to their parent.
   */
  @Test
  public void testBasicPanel() {
    this.tester.startComponentInPage(BasicPanel.class);
  }

  /**
   * Assert that {@link AutoWire#hasAutoComponentAnnotatedFields(Class class)} returns true for classes
   * with AutoComponent annotated fields.
   */
  @Test
  public void testAutoComponentMissingWithPanel() {
    this.tester.startComponentInPage(AutoComponentMissingInPanel.class);
    assertFalse(getAutoWire().hasAutoComponentAnnotatedFields(AutoComponentMissingInPanel.class));
  }

  /**
   * Assert that {@link AutoWire#hasAutoComponentAnnotatedFields(Class class)} returns true for classes
   * with AutoComponent annotated fields.
   */
  @Test
  public void testAutoComponentMissingInChildWithMarkupContainer() {
    this.tester.startPage(AutoComponentInParentMarkupContainer.class);
    AutoWire autoWire = getAutoWire();
    assertTrue(autoWire.hasAutoComponentAnnotatedFields(AutoComponentInParentMarkupContainer.class));
    assertFalse(autoWire.hasAutoComponentAnnotatedFields(AutoComponentInParentMarkupContainer.NestedContainer.class));
  }

  /**
   * Assert that {@link AutoWire#hasAutoComponentAnnotatedFields(Class class)} returns true for classes
   * with AutoComponent annotated fields.
   */
  @Test
  public void testAutoComponentMissingInParentWithMarkupContainer() {
    this.tester.startPage(AutoComponentInChildMarkupContainer.class);
    AutoWire autoWire = getAutoWire();
    assertFalse(autoWire.hasAutoComponentAnnotatedFields(AutoComponentInChildMarkupContainer.class));
    assertTrue(autoWire.hasAutoComponentAnnotatedFields(AutoComponentInChildMarkupContainer.NestedContainer.class));
  }

  /**
   * Assert that fields of parent class are processed too.
   */
  @Test
  public void testSubClassPage() {
    this.tester.startPage(SubClassPage.class);
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(SubClassPage.class));
  }

  private AutoWire getAutoWire() {
    for (IComponentInitializationListener listener : this.tester.getApplication().getComponentInitializationListeners()) {
      if (listener instanceof AutoWire) {
        return (AutoWire) listener;
      }
    }
    return null;
  }

  /**
   * Assert that child components are added to containers that do not have their
   * own markup, if the child component is a field of the panel that has
   * associated markup.
   */
  @Test
  public void testContainer() {
    this.tester.startPage(ContainerPage.class);
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(ContainerPage.class));
  }

  /**
   * Assert that components are automatically added to the border and the border
   * body.
   */
  @Test
  public void testBorder() {
    this.tester.startPage(BorderPage.class);
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(BorderPage.class));
  }

  /**
   * Assert that custom id annotation works. This is useful if the component id
   * is no a valid java identifier.
   */
  @Test
  public void testCustomId() {
    this.tester.startComponentInPage(CustomIdPanel.class);
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(CustomIdPanel.class));
  }

  /**
   * Assert that it is possible to overwrite the automatically created component
   * with a custom one.
   */
  @Test
  public void testManualInstantiation() {
    this.tester.startPage(ManualInstantiationPage.class);
    this.tester.assertLabel("test1", "test1");
    this.tester.assertLabel("test2", "test2");
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(ManualInstantiationPage.class));
  }

  /**
   * Assert that markup processing works for components with no close tag, for
   * example
   * 
   * <pre>
   *   <div>
   *     <br>
   *   </div>
   * </pre>
   */
  @Test
  public void testMissingCloseTag() {
    this.tester.startPage(MissingCloseTagPage.class);
    assertTrue(getAutoWire().hasAutoComponentAnnotatedFields(MissingCloseTagPage.class));
  }

  /**
   * Assert that child components are added to containers that do not have their
   * own markup, if the child component is a field of the container.
   */
  @Test
  public void testChildMarkupContainer() {
    this.tester.startPage(ChildMarkupContainer.class);
    AutoWire autoWire = getAutoWire();
    assertTrue(autoWire.hasAutoComponentAnnotatedFields(ChildMarkupContainer.class));
    assertTrue(autoWire.hasAutoComponentAnnotatedFields(ChildMarkupContainer.NestedContainer.class));
  }

  /**
   * Adds a loop to check if auto-wiring is too slow.
   */
  @Test
  public void testPerformance() {
    long begin = System.currentTimeMillis();
    this.tester.startPage(PerformanceTest.class);
    System.out.println("Performance test took " + (System.currentTimeMillis() - begin) + "ms");
  }

  /**
   * Generates a reference time for the performance-test without auto-wiring.
   */
  @Test
  public void testPerformanceCompare() {
    long begin = System.currentTimeMillis();
    new WicketTester().startPage(PerformanceCompareTest.class);
    System.out.println("Performance compare test took " + (System.currentTimeMillis() - begin) + "ms");
  }
}
