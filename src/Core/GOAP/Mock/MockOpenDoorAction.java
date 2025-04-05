package Core.GOAP.Mock;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.HashMap;
import java.util.Map;

public class MockOpenDoorAction implements Action {

    @Override
    public String getName() {
        return "MockOpenDoor_S0"; // Make name specific
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(WorldStateKey.TUT_CURRENT_SECTION, "S0_Start");
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        preconditions.put(WorldStateKey.S0_DOOR_OPEN, false);
        preconditions.put(WorldStateKey.TUT_S0_READY_FOR_DOOR, true); // <-- ADD THIS PRECONDITION
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(WorldStateKey.S0_DOOR_OPEN, true); // Use specific key
        effects.put(WorldStateKey.TUT_CURRENT_SECTION, "S1_Survival"); // Advance section
        return effects;
    }

    @Override
    public double getCost() { return 1.0; }

    @Override
    public boolean isApplicable(WorldState state) {
        boolean inSection = "S0_Start".equals(state.getString(WorldStateKey.TUT_CURRENT_SECTION));
        boolean dialogueClosed = !state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        boolean doorClosed = !state.getBoolean(WorldStateKey.S0_DOOR_OPEN);
        boolean readyForDoor = state.getBoolean(WorldStateKey.TUT_S0_READY_FOR_DOOR); // <-- CHECK READINESS
        return inSection && dialogueClosed && doorClosed && readyForDoor; // <-- ADD CHECK HERE
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        System.out.println("SIM: Performing " + getName());
        return ActionResult.SUCCESS;
    }
}
