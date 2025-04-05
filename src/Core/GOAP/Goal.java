package Core.GOAP; // Or your preferred package structure

import java.util.Map;
import java.util.Objects;

/**
 * Represents a goal state that the GOAP agent aims to achieve.
 * A goal is defined by a set of target conditions that must be met in the WorldState.
 */
public class Goal {

    private final String name;
    private final Map<WorldStateKey, Object> targetConditions;
    private final int priority; // Optional: Can be used later for prioritizing goals

    /**
     * Constructor for a Goal.
     *
     * @param name             A descriptive name for the goal (e.g., "Gather Logs", "Complete Cooking Section").
     * @param targetConditions A map where keys are WorldStateKeys and values are the desired states for those keys.
     * @param priority         An integer representing the goal's priority (higher value means higher priority). Set to 0 if not used initially.
     */
    public Goal(String name, Map<WorldStateKey, Object> targetConditions, int priority) {
        this.name = Objects.requireNonNull(name, "Goal name cannot be null");
        this.targetConditions = Objects.requireNonNull(targetConditions, "Target conditions cannot be null");
        this.priority = priority;

        if (targetConditions.isEmpty()) {
            // Consider if an empty goal is valid. Usually, a goal should have conditions.
            // For now, we allow it but could add validation later.
            // System.err.println("Warning: Goal '" + name + "' created with empty target conditions.");
        }
    }

    /**
     * Convenience constructor with default priority 0.
     */
    public Goal(String name, Map<WorldStateKey, Object> targetConditions) {
        this(name, targetConditions, 0);
    }

    /**
     * Gets the descriptive name of the goal.
     *
     * @return The goal name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the map of target conditions that define this goal.
     *
     * @return The map of target conditions.
     */
    public Map<WorldStateKey, Object> getTargetConditions() {
        // Consider returning an unmodifiable map if immutability is desired after creation
        return targetConditions;
    }

    /**
     * Gets the priority of this goal.
     *
     * @return The priority value.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Checks if the given WorldState satisfies all the target conditions of this goal.
     *
     * @param currentState The current WorldState to check against.
     * @return true if all target conditions are met in the currentState, false otherwise.
     */
    public boolean isSatisfied(WorldState currentState) {
        if (currentState == null) {
            return false; // Cannot satisfy a goal with a null state
        }
        // Delegate the check to the WorldState's satisfies method
        return currentState.satisfies(this.targetConditions);
    }

    // --- Equality and Hashing (Optional but good practice if storing Goals) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Goal goal = (Goal) o;
        // Goals are often considered equal if their names and conditions are the same.
        // Priority might or might not be part of equality depending on use case.
        return priority == goal.priority &&
                Objects.equals(name, goal.name) &&
                Objects.equals(targetConditions, goal.targetConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, targetConditions, priority);
    }

    // CHECK THIS METHOD TO MAKE SURE THE OUTPUT IS CORRECT, HAD ERROR AND REMOVED ONE ' FROM name + '\\''
    @Override
    public String toString() {
        return "Goal{" +
                "name='" + name + '\\' +
        ", priority=" + priority +
                ", conditions=" + targetConditions +
                '}';
    }
}
