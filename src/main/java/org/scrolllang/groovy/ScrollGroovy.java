package org.scrolllang.groovy;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.jetbrains.annotations.NotNull;
import org.scrolllang.groovy.bindings.ScriptBindings;
import org.scrolllang.groovy.bindings.ScrollGroovyBindings;
import org.scrolllang.scroll.Scroll;
import org.scrolllang.scroll.ScrollAddon;
import org.scrolllang.scroll.ScrollRegistration;import org.scrolllang.scroll.ScrollScriptLoader;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import org.codehaus.groovy.control.CompilationFailedException;
import net.fabricmc.api.ModInitializer;

public class ScrollGroovy extends ScrollAddon implements ModInitializer {

	private static final Map<String, Object> GLOBAL_BINDINGS = new HashMap<>();

	private static GroovyClassLoader GROOVY_CLASS_LOADER;
	private static GroovyScriptEngine SCRIPT_ENGINE;
	private static ScrollRegistration REGISTRATION;
	private static ScrollGroovy INSTANCE;
	private static Path SCRIPT_CLASSES;
	private static Path SCRIPT_LIBRARY;
	private static Path SCRIPTS_FOLDER;

	public ScrollGroovy() {
		super("scroll-groovy");
		if (INSTANCE == null) {
			INSTANCE = this;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void onInitialize() {
		SCRIPT_LIBRARY = getDataFolder().resolve("script-libraries");
		if (!Files.exists(SCRIPT_LIBRARY)) {
			try {
				Files.createDirectories(SCRIPT_LIBRARY);
			} catch (IOException e) {
				printException(e, "Failed to create the script libraries folder");
			}
		}

		SCRIPT_CLASSES = getDataFolder().resolve("script-classes");
		if (!Files.exists(SCRIPT_CLASSES)) {
			try {
				Files.createDirectories(SCRIPT_CLASSES);
			} catch (IOException e) {
				printException(e, "Failed to create the script classes folder");
			}
		}

		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.setTargetBytecode(CompilerConfiguration.JDK21);
		GLOBAL_BINDINGS.put("compilerConfiguration", configuration);
		GROOVY_CLASS_LOADER = new GroovyClassLoader(this.getClass().getClassLoader(), configuration);
	}

	/*
	 * This method is called by the Scroll if the addon
	 * was placed in the scroll/addons folder.
	 */
	public void initAddon() {
		onInitialize();
	}

	private boolean validateScriptAt(@NotNull Path script) {
		if (Files.isDirectory(script)) {
			error(Scroll.languageFormat("scripts.load.error.directory", script.toString()));
			return false;
		}
		String fileName = script.getFileName().toString();
		if (!fileName.endsWith(".groovy"))
			return false;
		return !fileName.startsWith(ScrollScriptLoader.DISABLED_PREFIX);
	}

	@Override
	protected void startRegistration(ScrollRegistration registration) {
		if (GROOVY_CLASS_LOADER == null)
			printException(new IllegalStateException(), "The Groovy class loader has not been initialized yet!");

		SCRIPTS_FOLDER = ScrollScriptLoader.getScriptsFolder();
		REGISTRATION = registration;

		GLOBAL_BINDINGS.put("addon", new ScrollGroovyBindings(this));
		GLOBAL_BINDINGS.put("registration", registration);

		long startingTime = System.currentTimeMillis();

		// Load script classes which run independently without any need of another script.
		ScrollScriptLoader.collectScriptsAt(SCRIPT_CLASSES, this::validateScriptAt).forEach(file -> {
			String scriptName = file.getFileName().toString();
			try {
				GROOVY_CLASS_LOADER.parseClass(file.toFile());
			} catch (IOException e) {
				printException(e, "Failed to access library script '" + scriptName + "'");
			} catch (CompilationFailedException e) {
				error(Scroll.languageFormat("scripts.parse.exception", scriptName));
				printException(e, "Failed to parse library script '" + scriptName + "'");
			}
		});

		try {
			URL[] urls = new URL[]{SCRIPTS_FOLDER.toUri().toURL()};
			SCRIPT_ENGINE = new GroovyScriptEngine(urls, GROOVY_CLASS_LOADER);
		} catch (IOException e) {
			printException(e, "Failed to create the Groovy script engine");
		}

		AtomicInteger scriptCount = new AtomicInteger(0);

		ScrollScriptLoader.collectScriptsAt(SCRIPT_LIBRARY, this::validateScriptAt).forEach(path -> {
			String scriptName = path.getFileName().toString();
			try {
				Binding binding = new Binding(GLOBAL_BINDINGS);
				Script script = SCRIPT_ENGINE.createScript(scriptName, binding);
				ScriptBindings scriptBindings = new ScriptBindings(SCRIPT_ENGINE, path, script);
				binding.setProperty("bindings", binding);
				binding.setProperty("script", scriptBindings);
				script.run();
				@SuppressWarnings("unchecked")
				Map<Object, Object> variables = binding.getVariables();
				variables.forEach((key, value) -> {
					if (key instanceof String string)
						GLOBAL_BINDINGS.put(string, value);
				});
				scriptCount.incrementAndGet();
			} catch (ResourceException e) {
				printException(e, "Failed to access library script '" + scriptName + "'");
			} catch (ScriptException e) {
				error(Scroll.languageFormat("scripts.parse.exception", scriptName));
				printException(e, "Failed to parse library script '" + scriptName + "'");
			} catch (MissingPropertyException e) {
				printException(e, "No property or missing import for '" + e.getProperty() + "' in library script " + scriptName);
			} catch (Exception e) {
				printException(e, Scroll.adventure("scripts.parse.exception", scriptName).getLiteralString());
			}
		});

		ScrollScriptLoader.collectScriptsAt(SCRIPTS_FOLDER, this::validateScriptAt).forEach(path -> {
			String scriptName = path.getFileName().toString();
			try {
				Binding binding = new Binding(GLOBAL_BINDINGS);
				Script script = SCRIPT_ENGINE.createScript(scriptName, binding);
				ScriptBindings scriptBindings = new ScriptBindings(SCRIPT_ENGINE, path, script);
				binding.setProperty("bindings", binding);
				binding.setProperty("script", scriptBindings);
				script.run();
				scriptCount.incrementAndGet();
			} catch (ResourceException e) {
				printException(e, "Failed to access script '" + scriptName + "'");
			} catch (ScriptException e) {
				error(Scroll.languageFormat("scripts.parse.exception", scriptName));
				printException(e, "Failed to parse script '" + scriptName + "'");
			} catch (MissingPropertyException e) {
				printException(e, "No property or missing import for '" + e.getProperty() + "' in script " + scriptName);
			} catch (Exception e) {
				printException(e, Scroll.adventure("scripts.parse.exception", scriptName).getLiteralString());
			}
		});
		info("Loaded " + scriptCount.get() + " groovy scripts in " + (System.currentTimeMillis() - startingTime) + "milliseconds.");
	}

	public static ScrollRegistration getRegistration() {
		return REGISTRATION;
	}

	public static ScrollGroovy getInstance() {
		if (INSTANCE == null)
			throw new IllegalStateException();
		return INSTANCE;
	}

}
