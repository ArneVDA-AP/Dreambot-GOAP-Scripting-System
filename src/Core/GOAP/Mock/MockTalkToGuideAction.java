package Core.GOAP.Mock;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.HashMap;
import java.util.Map;

public class MockTalkToGuideAction implements Action {

    private final String GUIDE_NAME = "RuneScape Guide";

    @Override
    public String getName() {
        return "MockTalkToGuide";
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(WorldStateKey.TUT_CURRENT_SECTION, "S0_Start"); // Example section value
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, true);
        effects.put(WorldStateKey.INTERACT_NPC_NAME, GUIDE_NAME); // Use the general key
        return effects;
    }

    @Override
    public double getCost() { return 1.0; }

    @Override
    public boolean isApplicable(WorldState state) {
        boolean inSection = "S0_Start".equals(state.getString(WorldStateKey.TUT_CURRENT_SECTION));
        boolean dialogueOpen = state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        return inSection && !dialogueOpen;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        System.out.println("SIM: Performing " + getName());
        return ActionResult.SUCCESS;
    }
}
