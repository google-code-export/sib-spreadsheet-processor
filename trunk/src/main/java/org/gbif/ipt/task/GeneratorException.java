package org.gbif.ipt.task;

/**
 * Exception thrown when generating a dwc archive fails.
 */
public class GeneratorException extends Exception {

  private static final long serialVersionUID = -4437160995679150094L;

  public GeneratorException(String message) {
    super(message);
  }

  public GeneratorException(String message, Throwable cause) {
    super(message, cause);
  }

  public GeneratorException(Throwable cause) {
    super(cause);
  }

}