package Core.GOAP.Mock; // Place this in your mock package, or a dedicated test package

import Core.GOAP.*; // Import your core GOAP classes

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A simple simulator to test the GOAP Planner logic in isolation,
 * using mock actions and states based on the start of Tutorial Island.
 */
public class GoapSimulator {

    public static void main(String[] args) {
        System.out.println("--- GOAP Planner Simulation Start ---");

        // 1. Define Available Mock Actions
        List<Action> availableActions = new ArrayList<>();
        availableActions.add(new MockTalkToGuideAction());
        availableActions.add(new MockContinueDialogueAction("RuneScape Guide")); // Ensure NPC name matches effect
        availableActions.add(new MockOpenDoorAction());
        System.out.println("Available Actions: " + availableActions.stream().map(Action::getName).collect(Collectors.toList()));

        // 2. Define the Initial World State
        WorldState initialState = new WorldState();
        initialState.setString(WorldStateKey.TUT_CURRENT_SECTION, "S0_Start");
        initialState.setBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false);
        initialState.setString(WorldStateKey.INTERACT_NPC_NAME, null);
        initialState.setBoolean(WorldStateKey.S0_DOOR_OPEN, false);
        initialState.setBoolean(WorldStateKey.TUT_S0_READY_FOR_DOOR, false);
        System.out.println("\\nInitial World State:\\n" + initialState);


        // 3. Define the Goal State
        Map<WorldStateKey, Object> goalConditions = new HashMap<>();
        goalConditions.put(WorldStateKey.TUT_CURRENT_SECTION, "S1_Survival");
        // Or alternatively: goalConditions.put(WorldStateKey.S0_DOOR_OPEN, true);
        Goal testGoal = new Goal("ReachSurvivalSection", goalConditions);
        System.out.println("\\nGoal State: " + testGoal);


        // 4. Instantiate the Planner
        Planner planner = new Planner();


        // 5. Run the Planner
        System.out.println("\\n--- Running Planner ---");
        Plan plan = planner.plan(initialState, testGoal, availableActions);


        // 6. Print the Result
        System.out.println("\\n--- Planner Result ---");
        if (plan != null && !plan.isEmpty()) {
            System.out.println("Plan Found:");
            System.out.println(plan); // Uses Plan.toString()
        } else {
            System.out.println("No plan found.");
        }

        System.out.println("\\n--- GOAP Planner Simulation End ---");
    }
}
