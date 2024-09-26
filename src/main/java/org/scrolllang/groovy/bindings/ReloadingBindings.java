package org.scrolllang.groovy.bindings;

// TODO currently FileReloading calls these methods. Make a universal method for reloading groovy scripts and error handling.
public interface ReloadingBindings {

	void onUnload();
	void onLoad();

}
