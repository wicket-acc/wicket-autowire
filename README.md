wicket-autowire [![Build Status](https://travis-ci.org/wicket-acc/wicket-autowire.png?branch=master)](https://travis-ci.org/wicket-acc/wicket-autowire)
=================================================================================================================================================

Annotation based auto wiring of wicket components. We deploy to maven central, so just add the following lines to your pom.xml:

	<dependency>
		<groupId>com.github.wicket-acc</groupId>
		<artifactId>wicket-autowire</artifactId>
		<version>0.0.3</version>
	</dependency>

How to use:
-----------

To enable in your wicket application, install wicket-autowire in your application's init() method:

	AutoWire.install(this);

Now components are built and added to page automatically and at the right place. The wicket id is taken from the field's name:

	public class BasicPanel extends Panel {
	
		private static final long serialVersionUID = 1L;
	
		@AutoComponent
		Label label;
	
		public BasicPanel(final String id) {
			super(id);
		}
	
	}

The only precondition for injection is a single argument constructor. For Links, that are typically anonymous inner classes, you can use regular inner classes.

How it works:
-------------

Wicket auto-wire works in two phases:

1. On construction, it injects the components of the annotated fields
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

* Eeach auto wired page must be a direct child of the declaring component or must have an auto-wired parent, that is also declared in the same component.
* It is not possible to access a field of the enclosing class in the constructor of an auto-wired non-static inner class.