package Core.GOAP; // Or your preferred package structure

/**
 * Represents the result of executing an Action's perform() method during a single game tick/loop.
 * This informs the ExecutionEngine how to proceed.
 */
public enum ActionResult {
    /**
     * The action completed successfully within this execution cycle.
     * The ExecutionEngine should proceed to the next action in the plan (if any).
     */
    SUCCESS,

    /**
     * The action failed to execute or complete its objective.
     * This could be due to unmet conditions discovered during execution,
     * API interaction failures, timeouts, or unexpected game states.
     * The ExecutionEngine should typically trigger a replan.
     */
    FAILURE,

    /**
     * The action has started or is continuing but requires more time (game ticks) to complete.
     * Examples include walking to a location, fishing, mining, combat, or waiting for an animation.
     * The ExecutionEngine should re-execute the *same* Action instance in the next loop,
     * according to the contract defined (Phase 1, Step 1.9).
     */
    IN_PROGRESS
}
