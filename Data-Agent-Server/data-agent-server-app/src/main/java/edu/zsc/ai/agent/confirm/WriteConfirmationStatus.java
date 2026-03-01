
package edu.zsc.ai.agent.confirm;

/**
 * Lifecycle states for a write confirmation token.
 */
public enum WriteConfirmationStatus {
    /** Awaiting user confirmation in the UI. */
    PENDING,
    /** User clicked "Confirm & Execute" in the UI. */
    CONFIRMED,
    /** Token was consumed by executeNonSelectSql; cannot be reused. */
    CONSUMED,
}
