package net.sibcolombia.sibsp.service;


/**
 * Exception thrown when importing a template file has failed.
 */
public class ImportException extends Exception {

  private static final long serialVersionUID = -5124494880728784846L;

  public ImportException(String message) {
    super(message);
  }

  public ImportException(String message, Throwable cause) {
    super(message, cause);
  }

  public ImportException(Throwable cause) {
    super(cause);
  }

}
