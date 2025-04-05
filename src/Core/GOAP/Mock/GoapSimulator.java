package Core.GOAP.Mock;

import Core.GOAP.*; // Import your core GOAP classes

import java.util.*; // Import util classes
import java.util.stream.Collectors; // Import Collectors

/**
 * A simple simulator to test the GOAP Planner and ExecutionEngine logic in isolation.
 */
public class GoapSimulator {

    public static void main(String[] args) {
        System.out.println("--- GOAP Simulation Start ---");

        // --- Test Scenario Setup ---
        WorldStateKey keyTask1Done = WorldStateKey.S0_DOOR_OPEN; // Reuse an existing key for simplicity
        WorldStateKey keyTask2Done = WorldStateKey.S1_HAS_AXE;   // Reuse another key

        List<Action> availableActions = new ArrayList<>();
        availableActions.add(new MockInProgressAction(3, keyTask1Done)); // Takes 3 ticks
        availableActions.add(new MockFailAction());
        availableActions.add(new MockGetItemAction("Axe", keyTask2Done, "Anywhere", WorldStateKey.LOC_CURRENT_AREA)); // Simple get item

        WorldState initialState = new WorldState();
        initialState.setBoolean(keyTask1Done, false);
        initialState.setBoolean(keyTask2Done, false);
        initialState.setString(WorldStateKey.LOC_CURRENT_AREA, "Anywhere"); // Needed for MockGetItemAction

        Map<WorldStateKey, Object> goalConditions = new HashMap<>();
        goalConditions.put(keyTask1Done, true);
        goalConditions.put(keyTask2Done, true);
        Goal testGoal = new Goal("CompleteInProgressAndGetItem", goalConditions);

        System.out.println("Available Actions: " + availableActions.stream().map(Action::getName).collect(Collectors.toList()));
        System.out.println("\\nInitial World State:\\n" + initialState);
        System.out.println("\\nGoal State: " + testGoal);

        // --- Planner ---
        Planner planner = new Planner();
        System.out.println("\\n--- Running Planner ---");
        Plan plan = planner.plan(initialState, testGoal, availableActions);

        if (plan == null || plan.isEmpty()) {
            System.out.println("\\n--- Planner Result ---");
            System.out.println("No plan found by planner. Cannot test engine.");
            System.out.println("\\n--- GOAP Simulation End ---");
            return;
        }
        System.out.println("\\n--- Planner Result ---");
        System.out.println("Plan Found:\\n" + plan);


        // --- Execution Engine ---
        ExecutionEngine engine = new ExecutionEngine();
        engine.setPlan(plan); // Give the plan to the engine

        System.out.println("\\n--- Running Execution Engine ---");
        int maxEngineSteps = 20; // Safety break for simulation
        int step = 0;
        ExecutionEngine.EngineStatus status = ExecutionEngine.EngineStatus.EXECUTING; // Initial status

        // Simulate the main loop calling executeNextStep
        while (step < maxEngineSteps && engine.isExecuting()) {
            step++;
            System.out.println("\\n--- Engine Step " + step + " ---");
            System.out.println("Current World State (Start of Step):\\n" + initialState); // Show state before execution
            System.out.println("Engine executing action: " + engine.getCurrentActionName());

            status = engine.executeNextStep(initialState); // Execute one step

            System.out.println("Engine Status after step: " + status);

            // --- Simulate World State Changes (for testing engine logic) ---
            // In a real script, the WorldObserver would update this based on game state.
            // Here, we manually apply effects *after* the action SUCCEEDS for simulation.
            // We only need to do this if the planner needs to run again based on the *actual* state.
            // For this test, we primarily care about the engine's reaction to ActionResult.

            // --- Test Replanning on Failure ---
            if (status == ExecutionEngine.EngineStatus.REPLAN_NEEDED) {
                System.out.println("--- Replanning Required ---");
                // Simulate failure recovery: Maybe try planning again?
                // For this test, we'll just stop. In a real script, you'd call planner.plan() again.
                System.out.println("Stopping simulation due to REPLAN_NEEDED status.");
                break; // Stop simulation after failure signal
            }

            // --- Test IN_PROGRESS Handling ---
            // No specific code needed here, the engine loop handles it by calling executeNextStep again.

            // Simulate a short delay between steps
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        System.out.println("\\n--- Execution Engine Finished ---");
        System.out.println("Final Status: " + status);
        System.out.println("Final World State:\\n" + initialState); // Show final state
        if (step >= maxEngineSteps) {
            System.out.println("Warning: Simulation hit max steps (" + maxEngineSteps + ").");
        }

        System.out.println("\\n--- GOAP Simulation End ---");
    }
}
