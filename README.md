wicket-autowire [![Build Status](https://travis-ci.org/wicket-acc/wicket-autowire.png?branch=master)](https://travis-ci.org/wicket-acc/wicket-autowire)
=================================================================================================================================================

Apache Wicket is a great framework for web applications, and the decoupling of logic and layout is good, with one limitation:
You have to "rebuild" the component hierarchy of your markup in your java code. Using this library makes your life easier:

* You don't have to add a component to its parent yourself.
* You can skip a component in the markup if you want to.
* You can simply have differenty styles with completely different component positions.
* You dont have to call the constructor of the component and pass the id as a string.

How to use:
-----------

Add the following lines to your pom.xml:

	<dependency>
		<groupId>com.github.wicket-acc</groupId>
		<artifactId>wicket-autowire</artifactId>
		<version>1.0.0</version>
	</dependency>

To enable auto-wire in your wicket application, install wicket-autowire in your application's init() method:

	AutoWire.install(this);

Now components are built and added to page automatically and at the right place. The wicket id is taken from the field's name:

	public class BasicPanel extends Panel {
	
		@AutoComponent
		Label label;
	
		public BasicPanel(final String id) {
			super(id);
		}
	
	}

How it works:
-------------

Wicket auto-wire works in two phases:

1. On construction, it injects components to the annotated fields
2. On initiation, it adds each component of an annoated field to its parent, corresponding to the html markup.

Customizing
-----------

If you want to create and assign components manually (for example if there is no suitable constructor), disable injection:

	@AutoComponent(inject=false)
	Link<?> link;

Sometimes the component id is not a valid java identifier. No problem, just annotate the id:

	@AutoComponent(id="like-button")
	LikeButton likeButton;

Limitations
-----------

* Each auto-wired component must be a direct child of the declaring component or must have an auto-wired parent, that is also declared in the same component.
* It is not possible to access a field of the enclosing class in the constructor of an auto-wired non-static inner class.
* Injected components need a constructor with wicket id as the only parameter. For Links, that are typically anonymous inner classes, you can use regular inner classes.
