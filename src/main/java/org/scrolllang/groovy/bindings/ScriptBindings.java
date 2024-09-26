package org.scrolllang.groovy.bindings;

import org.scrolllang.groovy.files.FileReloading;

import com.google.common.collect.Lists;

import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

public class ScriptBindings implements ReloadingBindings {

	private final List<Runnable> reloadHooks = new ArrayList<>();
	private final FileReloading fileReloading;
	private final Path scriptPath;
	private final Script script;

	public ScriptBindings(GroovyScriptEngine scriptEngine, Path scriptPath, Script script) {
		this.fileReloading = new FileReloading(this, scriptEngine, scriptPath);
		this.scriptPath = scriptPath;
		this.script = script;
	}

	public void addReloadHooks(Runnable... runnables) {
		reloadHooks.addAll(Lists.newArrayList(runnables));
	}

	public void setAutoReload(boolean reload) {
		fileReloading.isAutoReload();
  }

	public boolean isAutoReload() {
		return fileReloading.isAutoReload();
	}

	public Script getGroovyScript() {
		return script;
	}

	public Path getScriptPath() {
		return scriptPath;
	}

	@Override
	public void onLoad() {}

	@Override
	public void onUnload() {
		for (Runnable hook : reloadHooks) {
			hook.run();
		}
		reloadHooks.clear();
	}

}
