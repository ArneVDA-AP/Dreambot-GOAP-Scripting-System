package Core.GOAP.Mock;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.HashMap;
import java.util.Map;

public class MockContinueDialogueAction implements Action {

    private final String NPC_NAME;

    public MockContinueDialogueAction(String npcName) {
        this.NPC_NAME = npcName;
    }

    @Override
    public String getName() {
        return "MockContinueDialogue_" + NPC_NAME;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, true);
        preconditions.put(WorldStateKey.INTERACT_NPC_NAME, NPC_NAME); // Use general key
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        effects.put(WorldStateKey.INTERACT_NPC_NAME, null);
        effects.put(WorldStateKey.TUT_S0_READY_FOR_DOOR, true); // <-- ADD THIS EFFECT
        return effects;
    }

    @Override
    public double getCost() { return 1.0; }

    @Override
    public boolean isApplicable(WorldState state) {
        boolean dialogueOpen = state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN);
        boolean correctNPC = NPC_NAME.equals(state.getString(WorldStateKey.INTERACT_NPC_NAME));
        return dialogueOpen && correctNPC;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        System.out.println("SIM: Performing " + getName());
        return ActionResult.SUCCESS;
    }
}
