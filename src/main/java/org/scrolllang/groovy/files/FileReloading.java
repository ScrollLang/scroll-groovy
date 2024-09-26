package org.scrolllang.groovy.files;

import java.io.IOException;
import java.nio.file.*;

import org.scrolllang.groovy.ScrollGroovy;
import org.scrolllang.groovy.bindings.ScriptBindings;
import org.scrolllang.scroll.Scroll;

import groovy.lang.MissingPropertyException;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

public class FileReloading {

	private final GroovyScriptEngine scriptEngine;
	private final ScriptBindings scriptBindings;
	private final Path scriptPath;
	private boolean autoReload;

	public FileReloading(ScriptBindings scriptBindings, GroovyScriptEngine scriptEngine, Path scriptPath) {
		this.scriptBindings = scriptBindings;
		this.scriptEngine = scriptEngine;
		this.scriptPath = scriptPath;

		FileReloadingManager.add(this);
		startListening();
	}

	public void setAutoReload(boolean autoReload) {
		this.autoReload = autoReload;
	}

	public boolean isAutoReload() {
		return autoReload;
	}

	/**
	 * Called to reload the groovy script.
	 */
	public void reload() {
		if (!autoReload) { // TODO make the startListening thread not run if autoReload is false
			return;
		}
		scriptBindings.onUnload();
		String scriptName = scriptPath.getFileName().toString();
		ScrollGroovy instance = ScrollGroovy.getInstance();
		// TODO make a universal method for reloading groovy scripts and error handling.
		try {
			scriptEngine.loadScriptByName(scriptPath.toString());
		} catch (ResourceException e) {
			instance.printException(e, "Failed to access script '" + scriptName + "'");
		} catch (ScriptException e) {
			instance.error(Scroll.languageFormat("scripts.parse.exception", scriptName));
			instance.printException(e, "Failed to parse script '" + scriptName + "'");
		} catch (MissingPropertyException e) {
			instance.printException(e, "No property or missing import for '" + e.getProperty() + "' in script " + scriptName);
		} catch (Exception e) {
			instance.printException(e, Scroll.adventure("scripts.parse.exception", scriptName).getLiteralString());
		}
		scriptBindings.onLoad();
	}

	public void startListening() {
		Thread thread = new Thread(() -> {
			try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
				scriptPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

				while (true) {
					WatchKey key;
					try {
						key = watchService.take();
					} catch (InterruptedException ex) {
						return;
					}

					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();

						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}

						@SuppressWarnings("unchecked")
						WatchEvent<Path> ev = (WatchEvent<Path>) event;
						Path changed = ev.context();

						if (changed.equals(scriptPath.getFileName())) {
							reload();
						}
					}

					boolean valid = key.reset();
					if (!valid) {
						break;
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		thread.setDaemon(true);
		thread.start();
	}

}
