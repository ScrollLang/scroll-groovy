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
import org.scrolllang.groovy.bindings.AddonBindings;
import org.scrolllang.groovy.bindings.ScrollBindings;
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

	private final Map<String, Object> bindings = new HashMap<>();

	private static GroovyClassLoader GROOVY_CLASS_LOADER;
	private static GroovyScriptEngine SCRIPT_ENGINE;
	private static ScrollRegistration REGISTRATION;
	private static ScrollGroovy INSTANCE;
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

		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.setTargetBytecode(CompilerConfiguration.JDK21);
		bindings.put("compilerConfiguration", configuration);
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

		bindings.put("scroll", new ScrollBindings());
		bindings.put("addon", new AddonBindings());
		bindings.put("registration", registration);

		long startingTime = System.currentTimeMillis();
		AtomicInteger scriptCount = new AtomicInteger(0);

		// Load library scripts
		ScrollScriptLoader.collectScriptsAt(SCRIPT_LIBRARY, this::validateScriptAt).forEach(file -> {
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

		Binding binding = new Binding(bindings);
		ScrollScriptLoader.collectScriptsAt(SCRIPTS_FOLDER, this::validateScriptAt).forEach(file -> {
			String scriptName = file.getFileName().toString();
			try {
				Script script = SCRIPT_ENGINE.createScript(scriptName, binding);
				binding.setProperty("script", script);
				script.run();
				scriptCount.incrementAndGet();
			} catch (ResourceException e) {
				printException(e, "Failed to access script '" + scriptName + "'");
			} catch (ScriptException e) {
				error(Scroll.languageFormat("scripts.parse.exception", scriptName));
				printException(e, "Failed to parse script '" + scriptName + "'");
			} catch (MissingPropertyException e) {
				printException(e, "No property or missing import for '" + e.getProperty() + "' in class " + scriptName);
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
