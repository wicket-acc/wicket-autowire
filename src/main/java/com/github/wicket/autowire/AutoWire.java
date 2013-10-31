/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.wicket.autowire;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.IMarkupFragment;
import org.apache.wicket.markup.MarkupNotFoundException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.resolver.WicketContainerResolver;

public final class AutoWire implements IComponentInitializationListener {

  @Override
  public void onInitialize(final Component component) {
    if (component instanceof MarkupContainer && !(component instanceof TransparentWebMarkupContainer)) {
      try {
        IMarkupFragment markup = ((MarkupContainer) component).getMarkup(null);

        if (markup == null) {
          return;
        }

        final MarkupStream stream = new MarkupStream(markup);

        final Stack<AtomicReference<Component>> stack = new Stack<AtomicReference<Component>>();
        stack.push(new AtomicReference<Component>(component));

        // detect borders.
        boolean addToBorder = false;

        while (stream.skipUntil(ComponentTag.class)) {
          final ComponentTag tag = stream.getTag();
          // track border tags
          if (tag instanceof WicketTag) {
            if (((WicketTag) tag).isBorderTag() && tag.isOpen()) {
              addToBorder = true;
            }
            else if (((WicketTag) tag).isBodyTag() && tag.isOpen()) {
              addToBorder = false;
            }
            else if (((WicketTag) tag).isBodyTag() && tag.isClose()) {
              addToBorder = true;
            }
            else if (((WicketTag) tag).isBorderTag() && tag.isClose()) {
              addToBorder = false;
            }
          }
          // maintain bread crumbs and build components
          if (!(tag instanceof WicketTag) || tag.getName().equals(WicketContainerResolver.CONTAINER)) {
            if (tag.isOpen() || tag.isOpenClose()) {
              final Component container = stack.peek().get();
              final Component cmp;
              if (container == null) {
                cmp = null;
              }
              else {
                cmp = buildComponent(stack, tag.getId());
              }
              if (cmp != null) {
                if (container instanceof MarkupContainer) {
                  if (addToBorder && container instanceof Border) {
                    ((Border) container).addToBorder(cmp);
                  }
                  else {
                    ((MarkupContainer) container).add(cmp);
                  }
                }
                else if (container == null) {
                  throw new RuntimeException("component " + tag.getId()
                                             + " was auto wired, but its parent not!");
                }
                else {
                  throw new RuntimeException("only containers may contain child elements. type of "
                                             + container + " is not a container!");
                }
              }
              // push even if cmp is null, to track if parent is auto-wired
              if (tag.isOpen()) {
                stack.push(new AtomicReference<Component>(cmp));
              }
            }
            else if (tag.isClose()) {
              stack.pop();
            }
          }
          stream.next();
        }
        if (stack.size() != 1) {
          throw new RuntimeException("Stack must only contain one element " + stack);
        }

        if (component instanceof IAutoWireListener) {
          ((IAutoWireListener) component).onAutoWired();
        }
      }
      catch (final MarkupNotFoundException e) {
        return;
      }
    }
  }

  private Component buildComponent(final Stack<AtomicReference<Component>> stack, final String id) {
    Component instance = null;

    // every stack element could declare the desired child component as a field
    for (final AtomicReference<Component> tryal : stack) {
      Class<?> clazz = tryal.get().getClass();
      while (Component.class.isAssignableFrom(clazz)) {
        try {
          final Field field = clazz.getDeclaredField(id);
          if (field.isAnnotationPresent(com.github.wicket.autowire.Component.class)) {
            field.setAccessible(true);
            instance = (Component) field.get(tryal.get());
            if (instance == null) {
              instance = getInstance(field.getType(), stack, id);
              field.set(tryal.get(), instance);
            }
          }
        }
        catch (final NoSuchFieldException e) {
          // continue
        }
        catch (final SecurityException e) {
          throw new RuntimeException(e);
        }
        catch (final IllegalArgumentException e) {
          throw new RuntimeException(e);
        }
        catch (final IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        catch (final InstantiationException e) {
          throw new RuntimeException(e);
        }
        catch (final InvocationTargetException e) {
          throw new RuntimeException(e);
        }
        catch (final NoSuchMethodException e) {
          throw new RuntimeException(e);
        }
        clazz = clazz.getSuperclass();
      }
      if (instance != null) {
        break;
      }
    }
    return instance;
  }

  private Component getInstance(final Class<?> componentClass,
                                final Stack<AtomicReference<Component>> stack,
                                final String id) throws NoSuchMethodException,
                                                InstantiationException,
                                                IllegalAccessException,
                                                IllegalArgumentException,
                                                InvocationTargetException {
    if (componentClass.getEnclosingClass() == null) {
      // -- Static inner class or normal class
      final Constructor<?> constructor = componentClass.getDeclaredConstructor(String.class);
      constructor.setAccessible(true);
      return (Component) constructor.newInstance(id);
    }
    else {
      for (final AtomicReference<Component> enclosing : stack) {
        if (enclosing.get() != null
            && componentClass.getEnclosingClass().isAssignableFrom(enclosing.get().getClass())) {
          final Constructor<?> constructor = componentClass.getDeclaredConstructor(componentClass.getEnclosingClass(),
                                                                                   String.class);
          constructor.setAccessible(true);
          return (Component) constructor.newInstance(enclosing.get(), id);
        }
      }
      throw new RuntimeException("Unable to initialize inner class "
                                 + componentClass.getClass().getSimpleName() + " with id " + id
                                 + ". Enclosing class is not in the component hierarchy.");
    }
  }

}