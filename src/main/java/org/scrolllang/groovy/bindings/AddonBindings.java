package org.scrolllang.groovy.bindings;

import org.slf4j.Logger;
import org.scrolllang.groovy.ScrollGroovy;
import org.scrolllang.scroll.ScrollRegistration;
import org.scrolllang.scroll.exceptions.EmptyStacktraceException;

public class AddonBindings {

  public EmptyStacktraceException printException(Exception e, String message) {
    return ScrollGroovy.getInstance().printException(e, message);
  }

  public ScrollRegistration getRegistration() {
    return ScrollGroovy.getRegistration();
  }

  public Logger getLogger() {
    return ScrollGroovy.getInstance().getLogger();
  }

  public void error(String message) {
    ScrollGroovy.getInstance().error(message);
  }

  public void warn(String message) {
    ScrollGroovy.getInstance().warn(message);
  }

  public void info(String message) {
    ScrollGroovy.getInstance().info(message);
  }

}
