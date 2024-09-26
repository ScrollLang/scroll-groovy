package org.scrolllang.groovy.bindings;

public class SchedulerBindings implements ReloadingBindings {

	@Override
	public void onUnload() {
		throw new UnsupportedOperationException("Unimplemented method 'onUnload'");
	}

	@Override
	public void onLoad() {
		throw new UnsupportedOperationException("Unimplemented method 'onLoad'");
	}

	// TODO make schedulers that can be reloaded

}
