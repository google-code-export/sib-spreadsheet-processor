package net.sibcolombia.sibsp.service;

/**
 * Exception thrown when removing an entity is not allowed for some reason.
 */
public class DeletionNotAllowedException extends Exception {

  public enum Reason {
    LAST_ADMIN, LAST_RESOURCE_MANAGER, EXTENSION_MAPPED, BASE_VOCABULARY, VOCABULARY_USED_IN_EXTENSION,
    RESOURCE_REGISTERED_WITH_ORGANISATION, REGISTRY_ERROR
  }

  private static final long serialVersionUID = 7888238395111770501L;

  protected Reason reason;

  public DeletionNotAllowedException(Reason reason) {
    this.reason = reason;
  }

  public DeletionNotAllowedException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  /**
   * @return the reason why the deletion is not possible. This allows for internationalized display
   */
  public Reason getReason() {
    return reason;
  }
}