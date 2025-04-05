package Core.GOAP; // Or your preferred package structure

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the current state of the game world as perceived by the GOAP agent.
 * It uses a Map with WorldStateKey enums as keys for type safety and consistency.
 */
public class WorldState {

    private final Map<WorldStateKey, Object> state;

    public WorldState() {
        this.state = new HashMap<>();
    }

    // --- Boolean Accessors ---

    public boolean getBoolean(WorldStateKey key) {
        // Return false if key doesn't exist or is not a Boolean
        return (boolean) state.getOrDefault(key, false);
    }

    public void setBoolean(WorldStateKey key, boolean value) {
        state.put(key, value);
    }

    // --- Integer Accessors ---

    public int getInteger(WorldStateKey key) {
        // Return 0 if key doesn't exist or is not an Integer
        return (int) state.getOrDefault(key, 0);
    }

    public void setInteger(WorldStateKey key, int value) {
        state.put(key, value);
    }

    // --- String Accessors ---

    public String getString(WorldStateKey key) {
        // Return null if key doesn't exist or is not a String
        return (String) state.get(key); // Allow null for strings
    }

    public void setString(WorldStateKey key, String value) {
        if (value == null) {
            state.remove(key); // Or handle nulls as needed
        } else {
            state.put(key, value);
        }
    }

    // --- Generic Object Accessor (Use with caution) ---
    // Useful for complex types like Tile, Area, or custom Enums if needed

    public Object getObject(WorldStateKey key) {
        return state.get(key);
    }

    public void setObject(WorldStateKey key, Object value) {
        if (value == null) {
            state.remove(key);
        } else {
            state.put(key, value);
        }
    }

    // --- State Management ---

    /**
     * Checks if this WorldState contains all the conditions specified in another map.
     * Used by Actions to check preconditions and by Goals to check satisfaction.
     *
     * @param conditions A map representing the conditions to check (e.g., preconditions or goal state).
     * @return true if all conditions are met in this WorldState, false otherwise.
     */
    public boolean satisfies(Map<WorldStateKey, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // No conditions means it's satisfied
        }
        for (Map.Entry<WorldStateKey, Object> entry : conditions.entrySet()) {
            WorldStateKey key = entry.getKey();
            Object requiredValue = entry.getValue();
            Object actualValue = state.get(key);

            // If the key doesn't exist in the current state, it cannot satisfy the condition
            // unless the required value is specifically null (which is less common).
            // For simplicity, we often assume keys must exist and match.
            // Let's refine this: if a condition requires a specific value, the key must exist and match.
            // If a condition requires a key *not* to exist, that's harder to model directly here,
            // often handled by checking for a default value (like false for boolean, 0 for int).

            if (!state.containsKey(key)) {
                // Special case: if the condition requires 'false' and the key is missing, treat as false.
                if (requiredValue instanceof Boolean && !((Boolean) requiredValue)) {
                    continue; // Condition satisfied (key missing implies false)
                }
                // Special case: if the condition requires 0 and the key is missing, treat as 0.
                if (requiredValue instanceof Integer && ((Integer) requiredValue) == 0) {
                    continue; // Condition satisfied (key missing implies 0)
                }
                return false; // Key required by condition is missing
            }


            if (!Objects.equals(actualValue, requiredValue)) {
                return false; // Values don't match
            }
        }
        return true; // All conditions were met
    }

    /**
     * Applies the effects (changes) defined in a map to this WorldState.
     * Used by the Planner during state expansion.
     *
     * @param effects A map representing the changes to apply.
     */
    public void applyEffects(Map<WorldStateKey, Object> effects) {
        if (effects != null) {
            state.putAll(effects);
        }
    }

    /**
     * Creates a deep copy of this WorldState. Necessary for the planner
     * to explore different state branches without modifying the original state.
     *
     * @return A new WorldState instance with a copy of the internal map.
     */
    public WorldState copy() {
        WorldState newState = new WorldState();
        // Create a new HashMap by copying the entries from the current state
        newState.state.putAll(this.state);
        return newState;
    }


    // --- Equality and Hashing (Crucial for Planner's visited set) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldState that = (WorldState) o;
        // Two world states are equal if their internal maps are equal.
        return Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        // Hash code is based on the internal map's hash code.
        return Objects.hash(state);
    }

    // --- Debugging ---

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WorldState{\\n");
        for (Map.Entry<WorldStateKey, Object> entry : state.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
