package Core.GOAP.Mock;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.Collections;
import java.util.Map;

// Simple action that always fails
public class MockFailAction implements Action {
    @Override public String getName() { return "MockFailAction"; }
    @Override public Map<WorldStateKey, Object> getPreconditions() { return Collections.emptyMap(); }
    @Override public Map<WorldStateKey, Object> getEffects() { return Collections.emptyMap(); } // No effect on failure
    @Override public double getCost() { return 1.0; }
    @Override public boolean isApplicable(WorldState state) { return true; } // Always applicable

    @Override
    public ActionResult perform(WorldState currentState) {
        System.out.println("SIM: Performing " + getName() + " - Returning FAILURE");
        return ActionResult.FAILURE;
    }
}
