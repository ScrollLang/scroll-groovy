package org.scrolllang.groovy.bindings;

import org.slf4j.Logger;
import org.scrolllang.groovy.ScrollGroovy;
import org.scrolllang.scroll.ScrollRegistration;
import org.scrolllang.scroll.exceptions.EmptyStacktraceException;

public final class ScrollGroovyBindings {

  private final ScrollGroovy instance;

  public ScrollGroovyBindings(ScrollGroovy instance) {
    this.instance = instance;
  }

  public EmptyStacktraceException printException(Exception e, String message) {
    return instance.printException(e, message);
  }

  public ScrollRegistration getRegistration() {
    return ScrollGroovy.getRegistration();
  }

  public Logger getLogger() {
    return instance.getLogger();
  }

  public void error(String message) {
    instance.error(message);
  }

  public void warn(String message) {
    instance.warn(message);
  }

  public void info(String message) {
    instance.info(message);
  }

}
