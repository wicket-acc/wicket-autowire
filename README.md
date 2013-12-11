wicket-autowire [![Build Status](https://travis-ci.org/wicket-acc/wicket-autowire.png?branch=master)](https://travis-ci.org/wicket-acc/wicket-autowire)
=================================================================================================================================================

Annotation based auto wiring of wicket components. We deploy to maven central, so just add the following lines to your pom.xml:

	<dependency>
		<groupId>com.github.wicket-acc</groupId>
		<artifactId>wicket-autowire</artifactId>
		<version>0.0.3</version>
	</dependency>

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

If you want to create and assign components manually, disable injection:

	@AutoComponent(inject=false)
	Link<?> link;

Sometimes the component id is not suitable for a java identifier. No problem, just annotate the id:

	@AutoComponent(id="like-button")
	LikeButton likeButton;
