package net.sibcolombia.sibsp.service;

import java.io.IOException;

/**
 * Exception thrown when a source (file or sql) cannot be read.
 */
public class SourceException extends IOException {

  private static final long serialVersionUID = 7165754334131363691L;

  public SourceException(String message) {
    super(message);
  }

}