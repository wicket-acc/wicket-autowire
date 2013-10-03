wicket-autowire
===============

Annotation base auto wire of wicket components.

To enable, add the auto wire listener in your applicaion's init() method:

	getComponentInitializationListeners().add(new AutoWire());
  

With annotations:

	public class BasicPanel extends Panel {
	
		private static final long serialVersionUID = 1L;
	
		@Component
		Label label;
	
		public BasicPanel(final String id) {
			super(id);
		}
	
	}
	
Without annotations:
	
	public class BasicPanel extends Panel {
	
		private static final long serialVersionUID = 1L;
	
		public BasicPanel(final String id) {
			super(id);
		}
	
		@Override
		protected void onInitialize() {
			super.onInitialize();
			add(new Label("label"));
		}
	}
