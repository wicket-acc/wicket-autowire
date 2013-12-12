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

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;

import java.util.Arrays;

public class PerformanceTest extends WebPage {

  private static final long serialVersionUID = 1L;

  @AutoComponent(inject = false)
  ListView<?> loop;

  @Override
  protected void onInitialize() {
    super.onInitialize();

    loop = new ListView<Object>("loop", Arrays.asList(new Object[1000])) {
      @Override
      protected void populateItem(ListItem<Object> item) {
        item.add(new BasicPanel("panel"));
      }
    };
  }
}