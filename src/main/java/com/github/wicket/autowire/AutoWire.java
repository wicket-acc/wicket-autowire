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

public final class AutoWire implements IComponentInitializationListener {

	@Override
	public void onInitialize(final Component component) {
		if (component instanceof MarkupContainer && !(component instanceof TransparentWebMarkupContainer)) {
			try {
				final IMarkupFragment markup = ((MarkupContainer) component).getMarkup(null);

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
					else {
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
									throw new RuntimeException("component " + tag.getId() + " was auto wired, but its parent not!");
								}
								else {
									throw new RuntimeException("only containers may contain child elements. type of " + container + " is not a container!");
								}
							}
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
			try {
				final Field field = tryal.get().getClass().getDeclaredField(id);
				if (field.isAnnotationPresent(com.github.wicket.autowire.Component.class)) {
					field.setAccessible(true);
					// FIXME auto determine the enclosing type. can be any of the stack
					// elements
					instance = getInstance(field.getType(), tryal.get(), id);
					field.set(tryal.get(), instance);
				}
			}
			catch (final NoSuchFieldException e) {
				// continue
			}
			catch (final SecurityException e) {
				e.printStackTrace();
			}
			catch (final IllegalArgumentException e) {
				e.printStackTrace();
			}
			catch (final IllegalAccessException e) {
				e.printStackTrace();
			}
			catch (final InstantiationException e) {
				e.printStackTrace();
			}
			catch (final InvocationTargetException e) {
				e.printStackTrace();
			}
			catch (final NoSuchMethodException e) {
				e.printStackTrace();
			}

			if (instance != null) {
				break;
			}
		}
		return instance;
	}

	private Component getInstance(final Class<?> componentClass, final Component enclosingInstance, final String id) throws NoSuchMethodException, InstantiationException,
	    IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		try {
			// -- Static inner class or normal class
			final Constructor<?> constructor = componentClass.getDeclaredConstructor(String.class);
			constructor.setAccessible(true);
			return (Component) constructor.newInstance(id);
		}
		catch (final NoSuchMethodException e) {
			// -- Normal inner class
			final Constructor<?> constructor = componentClass.getDeclaredConstructor(enclosingInstance.getClass(), String.class);
			constructor.setAccessible(true);
			return (Component) constructor.newInstance(enclosingInstance, id);
		}
	}

}