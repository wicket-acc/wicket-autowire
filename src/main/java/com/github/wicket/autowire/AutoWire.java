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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.markup.*;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.resolver.WicketContainerResolver;

public final class AutoWire implements IComponentInitializationListener, IComponentInstantiationListener {

  private AutoWire() {

  }

  public static void install(final Application application) {
    final AutoWire instance = new AutoWire();
    application.getComponentInitializationListeners().add(instance);
    application.getComponentInstantiationListeners().add(instance);
  }

  @Override
  public void onInstantiation(final Component component) {
    if (isAutoWiringPossible(component)) {
      Class<?> clazz = component.getClass();
      @SuppressWarnings("unchecked")
      final List<AtomicReference<Component>> parent = Arrays.asList(new AtomicReference<Component>(component));
      while (Component.class.isAssignableFrom(clazz)) {
        for (final Field field : clazz.getDeclaredFields()) {
          if (field.isAnnotationPresent(AutoComponent.class)) {
            if (field.getAnnotation(AutoComponent.class).inject()) {
              String id = field.getAnnotation(AutoComponent.class).id();
              String name = id.isEmpty() ? field.getName() : id;
              buildComponent(parent, name);
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }
  }

  @Override
  public void onInitialize(final Component component) {
    if (isAutoWiringPossible(component)) {
      try {
        final IMarkupFragment markup = ((MarkupContainer) component).getMarkup(null);

        if (markup == null) {
          return;
        }

        final MarkupStream stream = new MarkupStream(markup);

        final Stack<AtomicReference<Component>> stack = new Stack<AtomicReference<Component>>();
        stack.push(new AtomicReference<Component>(component));

        // detect borders.
        boolean addToBorder = false;

        System.out.println("Performing auto wiring for component " + component);

        // no associated markup: component tag is part of the markup
        MarkupElement containerTag = null;
        // current criteria is fragile! find better way to check if component tag of component is part its markup.
        if (skipFirstComponentTag(component, stream)) {
          System.out.println("Skipped component tag " + stream.get());
          containerTag = stream.get();
          stream.next();
        }

        while (stream.skipUntil(ComponentTag.class)) {
          final ComponentTag tag = stream.getTag();

          System.out.println("Processing tag " + tag);

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

          System.out.println("addToBorder? " + addToBorder);

          // maintain bread crumbs and build components
          if (isComponentTag(tag)) {
            if (tag.isOpen() || tag.isOpenClose()) {
              final Component container = stack.peek().get();
              final Component cmp;

              System.out.println("Current parent component is " + container);
              if (container == null) {
                cmp = null;
              }
              else {
                cmp = buildComponent(stack, tag.getId());
              }

              System.out.println("Resolved component is " + cmp + ". Adding to parent now.");

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
              if (tag.isOpen() && !tag.hasNoCloseTag()) {
                System.out.println("Tag has a body. Adding to stack now.");
                stack.push(new AtomicReference<Component>(cmp));
                System.out.println("Current stack: " + stack);
              }
            }
            else if (tag.isClose() && !tag.getOpenTag().isAutoComponentTag()) {
              // the container tag is part of the inherited markup. do not pop stack on container tag close.
              if (containerTag == null || !tag.closes(containerTag)) {
                System.out.println("Tag is closing. Pop the stack now.");
                stack.pop();
                System.out.println("Current stack: " + stack);
              }
            }
          }
          System.out.println("--- Tag done. ---");
          stream.next();
        }
        if (stack.size() != 1) {
          throw new RuntimeException("Stack must only contain one element " + stack);
        }

      }
      catch (final MarkupNotFoundException e) {
        return;
      }
    }
  }

  private boolean skipFirstComponentTag(Component component, MarkupStream stream) {
    if (stream.get() instanceof ComponentTag
        && ((ComponentTag) stream.get()).getId().equals(component.getId())) {
      return true;
    }
    else if (component instanceof ListItem) {
      return true;
    }
    else {
      return false;
    }
  }

  private boolean isComponentTag(ComponentTag tag) {
    return !(tag instanceof WicketTag) && !tag.isAutoComponentTag()
           || tag.getName().equals(WicketContainerResolver.CONTAINER);
  }

  private boolean isAutoWiringPossible(final Component component) {
    return component instanceof MarkupContainer && !(component instanceof TransparentWebMarkupContainer);
  }

  private Component buildComponent(final Iterable<AtomicReference<Component>> stack, final String id) {
    Component instance = null;

    // every stack element could declare the desired child component as a field
    for (final AtomicReference<Component> tryal : stack) {
      Class<?> clazz = tryal.get().getClass();
      while (Component.class.isAssignableFrom(clazz)) {
        try {
          Field field = null;
          // look for annotated field
          for (Field iter : clazz.getDeclaredFields()) {
            if (iter.isAnnotationPresent(AutoComponent.class)) {
              AutoComponent ann = iter.getAnnotation(AutoComponent.class);
              if (ann.id().equals(id)) {
                field = iter;
                break;
              }
              iter.setAccessible(true);
              Component value = (Component) iter.get(tryal.get());
              if (value != null && value.getId().equals(id)) {
                field = iter;
                break;
              }
            }
          }
          // fallback to named field
          if (field == null) {
            field = clazz.getDeclaredField(id);
          }
          if (field.isAnnotationPresent(AutoComponent.class)) {
            field.setAccessible(true);
            instance = (Component) field.get(tryal.get());
            if (instance == null) {
              if (field.getAnnotation(AutoComponent.class).inject()) {
                instance = getInstance(field.getType(), stack, id);
                setValue(instance, tryal.get(), field);
              }
              else {
                throw new RuntimeException("Field " + field.getName() + " must be assigned manually!");
              }
            }
            if (instance != null) {
              return instance;
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
    }
    return null;
  }

  // set value on duplicated field of parend classes too!
  private void setValue(Component instance, final Component component, Field field) throws IllegalAccessException {
    Class<?> clazz = field.getDeclaringClass();
    while (Component.class.isAssignableFrom(clazz)) {
      for (Field f : clazz.getDeclaredFields()) {
        if (f.getName().equals(field.getName())) {
          f.setAccessible(true);
          f.set(component, instance);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private Component getInstance(final Class<?> componentClass,
                                final Iterable<AtomicReference<Component>> stack,
                                final String id) throws NoSuchMethodException,
                                                InstantiationException,
                                                IllegalAccessException,
                                                IllegalArgumentException,
                                                InvocationTargetException {
    if (componentClass.getEnclosingClass() == null || Modifier.isStatic(componentClass.getModifiers())) {
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