package Core.GOAP; // Or your preferred package structure

import java.util.Map;

/**
 * Represents a single, discrete action that the GOAP agent can perform
 * to change the state of the world. Actions have preconditions that must be met,
 * effects that describe how they change the world state upon successful completion,
 * a cost associated with performing them, and the logic to execute the action in-game.
 */
public interface Action {

    /**
     * Gets the unique and descriptive name of this action.
     * Useful for logging and debugging.
     *
     * @return The name of the action (e.g., "WalkToFishingArea", "CatchShrimp").
     */
    String getName();

    /**
     * Gets the conditions that must be true in the WorldState for this action
     * to be considered applicable by the Planner.
     *
     * @return A Map where keys are WorldStateKeys and values are the required states.
     *         An empty map means the action has no preconditions (always applicable if effects are useful).
     */
    Map<WorldStateKey, Object> getPreconditions();

    /**
     * Gets the changes to the WorldState that are expected to occur if this action
     * completes successfully. Used by the Planner to simulate state transitions.
     * Note: This represents the *intended* outcome, not the guaranteed outcome during execution.
     *
     * @return A Map where keys are WorldStateKeys and values are the resulting states after the action.
     */
    Map<WorldStateKey, Object> getEffects();

    /**
     * Gets the cost associated with performing this action.
     * The Planner uses this to find the most efficient (lowest cost) plan.
     * Cost can represent time, resource consumption, risk, etc.
     * For MVP, a uniform cost of 1.0 is often sufficient.
     *
     * @return The cost of the action as a double.
     */
    double getCost();

    /**
     * Checks if this action is currently applicable given the provided WorldState.
     * This typically involves checking if the state satisfies the action's preconditions.
     * Can also include runtime checks not easily represented as simple preconditions
     * (e.g., checking reachability of a target).
     *
     * @param state The current WorldState.
     * @return true if the action can be considered for planning or execution, false otherwise.
     */
    boolean isApplicable(WorldState state);

    /**
     * Executes the action logic within the game using the DreamBot API.
     * This method contains the actual interaction with the game (clicks, walking, etc.).
     * It should handle the progression of the action over potentially multiple game ticks
     * by returning the appropriate ActionResult.
     *
     * @param currentState The current WorldState, which might be needed for context during execution
     *                     (though ideally, applicability is checked beforehand).
     * @return ActionResult indicating the outcome of this execution cycle:
     *         - SUCCESS: The action completed its objective in this cycle.
     *         - FAILURE: The action failed and cannot be completed (e.g., timeout, unexpected state).
     *         - IN_PROGRESS: The action is ongoing and requires further execution cycles.
     */
    ActionResult perform(WorldState currentState);

    /**
     * Optional: Called when the ExecutionEngine starts executing this action instance.
     * Can be used for initialization specific to an execution attempt.
     */
    default void onStart() {
        // Default implementation does nothing
    }

    /**
     * Optional: Called if the action successfully completes (returns SUCCESS).
     * Can be used for cleanup or logging specific to successful completion.
     */
    default void onSuccess() {
        // Default implementation does nothing
    }

    /**
     * Optional: Called if the action fails (returns FAILURE).
     * Can be used for cleanup or logging specific to failure.
     */
    default void onFailure() {
        // Default implementation does nothing
    }

    /**
     * Optional: Called if the action is aborted by the ExecutionEngine (e.g., due to replan).
     * Can be used for cleanup if the action was interrupted mid-execution.
     */
    default void onAbort() {
        // Default implementation does nothing
    }

}
