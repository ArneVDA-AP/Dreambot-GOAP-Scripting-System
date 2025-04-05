package Core.GOAP.Mock;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// Simple action that takes a few 'ticks' to complete
public class MockInProgressAction implements Action {
    private int ticksRemaining;
    private final int totalTicks;
    private final WorldStateKey completionKey; // A key this action sets upon completion

    public MockInProgressAction(int ticksToComplete, WorldStateKey completionKey) {
        this.totalTicks = ticksToComplete;
        this.ticksRemaining = ticksToComplete;
        this.completionKey = completionKey;
    }

    @Override public String getName() { return "MockInProgressAction (" + ticksRemaining + "/" + totalTicks + ")"; }
    @Override public Map<WorldStateKey, Object> getPreconditions() { return Collections.emptyMap(); }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Effect: Sets the completion key to true
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(completionKey, true);
        return effects;
    }

    @Override public double getCost() { return 1.0 * totalTicks; } // Cost might reflect duration
    @Override public boolean isApplicable(WorldState state) { return !state.getBoolean(completionKey); } // Only applicable if not already done

    @Override
    public ActionResult perform(WorldState currentState) {
        System.out.println("SIM: Performing " + getName());
        if (ticksRemaining > 0) {
            ticksRemaining--;
            System.out.println("SIM: -> Ticks remaining: " + ticksRemaining + ". Returning IN_PROGRESS");
            return ActionResult.IN_PROGRESS;
        } else {
            System.out.println("SIM: -> Ticks complete. Returning SUCCESS");
            // Reset for potential reuse if needed, though engine usually gets a new plan
            // ticksRemaining = totalTicks;
            return ActionResult.SUCCESS;
        }
    }

    @Override
    public void onAbort() {
        System.out.println("SIM: " + getName() + " aborted!");
        // Reset state if aborted mid-execution
        ticksRemaining = totalTicks;
    }
}
