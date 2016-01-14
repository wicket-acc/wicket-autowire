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

import static java.util.Map.Entry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.application.IComponentInstantiationListener;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.IMarkupFragment;
import org.apache.wicket.markup.MarkupElement;
import org.apache.wicket.markup.MarkupNotFoundException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.resolver.WicketContainerResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoWire implements IComponentInitializationListener, IComponentInstantiationListener {

  private static final Logger log = LoggerFactory.getLogger(AutoWire.class);
  private static final ComponentCache cache = new ComponentCache();

  private AutoWire() {
  }

  public static void install(final Application application) {
    final AutoWire instance = new AutoWire();
    application.getComponentInitializationListeners().add(instance);
    application.getComponentInstantiationListeners().add(instance);
  }

  @Override
  public void onInstantiation(final Component component) {
    Value value = cache.get(component.getClass());
    if (value == null) {
      if (log.isTraceEnabled()) {
        log.trace("Cache miss");
      }

      synchronized (AutoWire.class) {
        value = cache.get(component.getClass());
        if (value == null) {
          value = getInstantiationActions(component);
          cache.put(component.getClass(), value);
        }
      }
    }
    value.performInstantiationActions(component);
  }

  boolean hasAutoComponentAnnotatedFields(Class clazz) {
    synchronized (AutoWire.class) {
      return cache.get(clazz).hasAutoComponentAnnotatedFields;
    }
  }

  private Value getInstantiationActions(Component component) {
    List<Action> actions = new ArrayList<Action>();
    boolean foundAnnotationAutoComponent = false;

    if (isAutoWiringPossible(component)) {
      Set<String> done = new HashSet<String>();
      Class<?> clazz = component.getClass();
      // iterate over class hierarchy
      while (Component.class.isAssignableFrom(clazz)) {
        if (log.isTraceEnabled()) {
          log.trace("looking for fields in class " + clazz);
        }
        // iterate over declared fields
        for (final Field field : clazz.getDeclaredFields()) {
          if (field.isAnnotationPresent(AutoComponent.class)) {
            foundAnnotationAutoComponent = true;
            AutoComponent ann = field.getAnnotation(AutoComponent.class);
            if (ann.inject()) {
              final String id = ann.id().isEmpty() ? field.getName() : ann.id();
              // fields in super classes are ignored, if they are in subclasses too
              if (!done.contains(id)) {
                done.add(id);
                Component value = getValue(component, field);
                if (value == null) {
                  actions.add(new AssignInstanceAction(field, id));
                }
                else {
                  if (log.isTraceEnabled()) {
                    log.trace("Field " + field.getName() + " is already initialized. skipping.");
                  }
                }
              }
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }

    if (log.isTraceEnabled()) {
      log.trace("Actions: " + actions);
    }

    return new Value(actions, foundAnnotationAutoComponent);
  }

  private static Component getValue(Component component, Field field) {
    boolean accessible = field.isAccessible();
    field.setAccessible(true);
    try {
      Component value = (Component) field.get(component);
      field.setAccessible(accessible);
      return value;
    }
    catch (IllegalAccessException e) {
      return null;
    }
  }

  @Override
  public void onInitialize(final Component component) {
    if (isAutoWiringPossible(component)) {
      try {
        Value value = cache.get(component.getClass());
        value.performInitializeActions(component);
      }
      catch (final MarkupNotFoundException e) {
        //Nothing to do
      }
    }
  }

  private boolean isAutoWiringPossible(final Component component) {
    return component instanceof MarkupContainer && !(component instanceof TransparentWebMarkupContainer);
  }

  private static Component buildComponent(Component component, final String id, Node child) {
    Class<?> clazz = component.getClass();
    while (Component.class.isAssignableFrom(clazz)) {
      Component value = null;
      // look for annotated field
      for (Field iter : clazz.getDeclaredFields()) {
        if (iter.isAnnotationPresent(AutoComponent.class)) {
          value = getValue(component, iter);
          if (value != null && value.getId().equals(id)) {
            child.field = iter;
            break;
          }
          else {
            value = null;
          }
        }
      }
      if (value != null) {
        return value;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  // set value on duplicated field of parent classes too!
  private static void setValue(Component instance,
                               final Component component,
                               Field field) throws IllegalAccessException {
    Class<?> clazz = field.getDeclaringClass();
    while (Component.class.isAssignableFrom(clazz)) {
      for (Field f : clazz.getDeclaredFields()) {
        if (f.getName().equals(field.getName())) {
          boolean accessible = f.isAccessible();
          f.setAccessible(true);
          f.set(component, instance);
          f.setAccessible(accessible);
        }
      }
      clazz = clazz.getSuperclass();
    }
  }

  private Component getInstance(final Class<?> componentClass,
                                final Component enclosing,
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
      if (enclosing != null && componentClass.getEnclosingClass().isAssignableFrom(enclosing.getClass())) {
        final Constructor<?> constructor = componentClass.getDeclaredConstructor(componentClass.getEnclosingClass(),
                                                                                 String.class);
        constructor.setAccessible(true);
        return (Component) constructor.newInstance(enclosing, id);
      }
      throw new RuntimeException("Unable to initialize inner class "
                                 + componentClass.getClass().getSimpleName() + " with id " + id
                                 + ". Enclosing class is not in the component hierarchy.");
    }
  }

  private static class Value {

    private static final int THRESHOLD_MILLIS = 8 * 24 * 60 * 60 * 1000;

    private final Map<String, Node> cache = new ConcurrentHashMap<String, Node>();
    private final List<Action> instantiationActions;
    private final boolean hasAutoComponentAnnotatedFields;

    public Value(List<Action> instantiationActions, boolean hasAutoComponentAnnotatedFields) {
      this.instantiationActions = instantiationActions;
      this.hasAutoComponentAnnotatedFields = hasAutoComponentAnnotatedFields;
    }

    public void performInstantiationActions(Component component) {
      for (Action action : instantiationActions) {
        action.perform(component);
      }
    }

    public void performInitializeActions(Component component) {
      if (!hasAutoComponentAnnotatedFields) {
        return;
      }

      final IMarkupFragment markup = ((MarkupContainer) component).getMarkup(null);

      if (markup == null) {
        return;
      }

      String key = markup.toString(false);
      Node node = cache.get(key);
      if (node == null) {
        if (log.isTraceEnabled()) {
          log.trace("MARKUP MISS");
        }
        synchronized (AutoWire.class) {
          node = cache.get(key);
          if (node == null) {
            node = getNode(component, markup);
            cache.put(key, node);
          }
        }
      }

      node.lastUsed = System.currentTimeMillis();
      node.initialize(component);

      cleanup();
    }

    // avoid memory leaks if markup changes often.
    private void cleanup() {
      if (cache.size() > 30) {
        long threshold = System.currentTimeMillis() - THRESHOLD_MILLIS;
        for (Iterator<Entry<String, Node>> iterator = cache.entrySet().iterator(); iterator.hasNext();) {
          Entry<String, Node> next = iterator.next();
          if (next.getValue().lastUsed < threshold) {
            iterator.remove();
          }
        }
      }
    }

    private Node getNode(Component component, IMarkupFragment markup) {

      final MarkupStream stream = new MarkupStream(markup);

      final Stack<AtomicReference<Component>> stack = new Stack<AtomicReference<Component>>();
      stack.push(new AtomicReference<Component>(component));

      Node node = new Node();

      // detect borders.
      boolean addToBorder = false;

      if (log.isTraceEnabled()) {
        log.trace("Performing auto wiring for component " + component);
      }

      // no associated markup: component tag is part of the markup
      MarkupElement containerTag = null;
      //TODO current criteria is fragile! find better way to check if component tag of component is part its markup.
      if (skipFirstComponentTag(component, stream)) {
        if (log.isTraceEnabled()) {
          log.trace("Skipped component tag " + stream.get());
        }
        containerTag = stream.get();
        stream.next();
      }

      while (stream.skipUntil(ComponentTag.class)) {
        final ComponentTag tag = stream.getTag();

        if (log.isTraceEnabled()) {
          log.trace("Processing tag " + tag);
        }

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

        if (log.isTraceEnabled()) {
          log.trace("addToBorder? " + addToBorder);
        }

        // maintain bread crumbs and build components
        if (isComponentTag(tag)) {
          if (tag.isOpen() || tag.isOpenClose()) {
            final Component container = stack.peek().get();
            final Component cmp;
            final Node child = new Node();

            if (log.isTraceEnabled()) {
              log.trace("Current parent component is " + container);
            }
            if (container == null) {
              cmp = null;
            }
            else {
              cmp = buildComponent(component, tag.getId(), child);
            }

            if (log.isTraceEnabled()) {
              log.trace("Resolved component is " + cmp + ". Adding to parent now.");
            }

            if (cmp != null) {
              if (container instanceof MarkupContainer) {
                if (addToBorder && container instanceof Border) {
                  child.border = true;
                }
                else {
                  child.border = false;
                }
                child.id = cmp.getId();
                node.add(child);
              }
              else if (container == null) {
                throw new RuntimeException("component " + tag.getId()
                                           + " was auto wired, but its parent not!");
              }
              else {
                throw new RuntimeException("only containers may contain child elements. type of " + container
                                           + " is not a container!");
              }
            }
            // push even if cmp is null, to track if parent is auto-wired
            if (tag.isOpen() && !tag.hasNoCloseTag()) {
              if (log.isTraceEnabled()) {
                log.trace("Tag has a body. Adding to stack now.");
              }
              stack.push(new AtomicReference<Component>(cmp));
              if (cmp != null) {
                node = child;
              }
              if (log.isTraceEnabled()) {
                log.trace("Current stack: " + stack);
              }
            }
          }
          else if (tag.isClose() && !tag.getOpenTag().isAutoComponentTag()) {
            // the container tag is part of the inherited markup. do not pop stack on container tag close.
            if (containerTag == null || !tag.closes(containerTag)) {
              if (log.isTraceEnabled()) {
                log.trace("Tag is closing. Pop the stack now.");
              }
              if (stack.pop().get() != null) {
                node = node.parent;
              }
              if (log.isTraceEnabled()) {
                log.trace("Current stack: " + stack);
              }
            }
          }
        }
        if (log.isTraceEnabled()) {
          log.trace("--- Tag done. ---");
        }
        stream.next();
      }
      if (stack.size() != 1) {
        throw new RuntimeException("Stack must only contain one element " + stack);
      }

      return node;
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

  }

  private static class ComponentCache extends ConcurrentHashMap<Class<? extends Component>, Value> {

  }

  private static class Node {

    Node parent = null;
    Field field = null;
    List<Node> childNodes = new ArrayList<Node>();
    boolean border = false;
    public String id = null;
    long lastUsed = System.currentTimeMillis();

    public void add(Node child) {
      child.parent = this;
      childNodes.add(child);
    }

    @Override
    public String toString() {
      return "Node{" + "field=" + ((field != null) ? field.getName() : null) + ", childNodes=" + childNodes
             + ", border=" + border + ", id='" + id + '\'' + '}';
    }

    public void initialize(Component component) {
      initialize(component, component);
    }

    private void initialize(Component root, Component parent) {
      for (Node child : childNodes) {
        Component value = getValue(root, child.field);
        if (child.border) {
          ((Border) parent).addToBorder(value);
        }
        else {
          ((MarkupContainer) parent).add(value);
        }
        if (!child.childNodes.isEmpty()) {
          child.initialize(root, value);
        }
      }
    }

  }

  private interface Action {
    void perform(Component component);
  }

  private class AssignInstanceAction implements Action {

    private final Field field;
    private final String id;

    public AssignInstanceAction(Field field, String id) {
      this.field = field;
      this.id = id;
    }

    @Override
    public String toString() {
      return "Assign instance with id " + id + " to field " + field.getName();
    }

    @Override
    public void perform(Component component) {
      try {
        Component instance = getInstance(field.getType(), component, id);
        setValue(instance, component, field);
      }
      catch (NoSuchMethodException e) {
        throw new WicketRuntimeException(e);
      }
      catch (InstantiationException e) {
        throw new WicketRuntimeException(e);
      }
      catch (IllegalAccessException e) {
        throw new WicketRuntimeException(e);
      }
      catch (InvocationTargetException e) {
        throw new WicketRuntimeException(e);
      }
    }
  }
}
