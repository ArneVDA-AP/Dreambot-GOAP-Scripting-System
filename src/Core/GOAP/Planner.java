package Core.GOAP; // Or your preferred package structure

import java.util.*;

/**
 * Implements a GOAP planner using Breadth-First Search (BFS).
 * Finds the shortest plan (in terms of number of actions) to achieve a goal state
 * from a given initial state, considering a set of available actions.
 */
public class Planner {

    private static final int MAX_ITERATIONS = 1000; // Safety limit to prevent infinite loops

    // Helper class to represent nodes in the search space
    private static class PlanNode {
        final WorldState state;
        final PlanNode parent; // Node from which this node was reached
        final Action action;   // Action taken to reach this state from the parent

        PlanNode(WorldState state, PlanNode parent, Action action) {
            this.state = state;
            this.parent = parent;
            this.action = action;
        }

        @Override
        public String toString() {
            // Simple representation for debugging
            return "PlanNode{action=" + (action != null ? action.getName() : "START") + ", stateHash=" + state.hashCode() + '}';
        }
    }

    /**
     * Attempts to find a sequence of actions (a plan) to reach the goal state.
     *
     * @param initialState     The starting state of the world.
     * @param goal             The desired goal state.
     * @param availableActions The list of all possible actions the agent can perform.
     * @return A Plan object containing the sequence of actions, or an empty Plan if no solution is found.
     */
    public Plan plan(WorldState initialState, Goal goal, List<Action> availableActions) {
        System.out.println("PLANNER: Starting planning..."); // Simple logging for now
        System.out.println("PLANNER: Initial State Hash: " + initialState.hashCode());
        System.out.println("PLANNER: Goal: " + goal.getName());

        Queue<PlanNode> openSet = new LinkedList<>();
        Set<WorldState> closedSet = new HashSet<>(); // Uses WorldState's hashCode/equals

        // 1.a Initialization
        PlanNode startNode = new PlanNode(initialState, null, null);
        openSet.add(startNode);
        closedSet.add(initialState);

        int iterations = 0;

        // 1.b Loop
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            // 1.b.i Dequeue
            PlanNode currentNode = openSet.poll();
            iterations++;

            //System.out.println("PLANNER: Iteration " + iterations + ", Exploring node: " + currentNode + ", OpenSet size: " + openSet.size());


            // 1.b.ii Goal Check
            if (goal.isSatisfied(currentNode.state)) {
                System.out.println("PLANNER: Goal found after " + iterations + " iterations!");
                return reconstructPlan(currentNode);
            }

            // 1.b.iii Expand Node
            for (Action action : availableActions) {
                // 1. Check Applicability
                if (action.isApplicable(currentNode.state)) {
                    // 2. Simulate Effect
                    WorldState nextState = currentNode.state.copy(); // Create a copy to modify
                    nextState.applyEffects(action.getEffects());

                    // 3. Cycle Check (using closedSet)
                    if (!closedSet.contains(nextState)) {
                        // 4. Enqueue
                        closedSet.add(nextState); // Mark this state as visited
                        PlanNode nextNode = new PlanNode(nextState, currentNode, action);
                        openSet.add(nextNode);
                        // System.out.println("PLANNER: Added node via action '" + action.getName() + "'. New state hash: " + nextState.hashCode());
                    } else {
                        // System.out.println("PLANNER: State already visited, skipping action '" + action.getName() + "'. State hash: " + nextState.hashCode());
                    }
                }
            }
        }

        // 1.c Failure
        if (iterations >= MAX_ITERATIONS) {
            System.err.println("PLANNER: Failed to find plan - Max iterations reached (" + MAX_ITERATIONS + ")");
        } else {
            System.err.println("PLANNER: Failed to find plan - Open set became empty after " + iterations + " iterations.");
        }
        return new Plan(new LinkedList<>()); // Return an empty plan on failure
    }

    /**
     * Reconstructs the plan by backtracking from the goal node to the start node.
     *
     * @param goalNode The PlanNode representing the state where the goal was satisfied.
     * @return A Plan object containing the sequence of actions in execution order.
     */
    private Plan reconstructPlan(PlanNode goalNode) {
        LinkedList<Action> actions = new LinkedList<>();
        PlanNode current = goalNode;
        while (current != null && current.parent != null) { // Stop when we reach the start node (which has null parent/action)
            if (current.action != null) {
                actions.addFirst(current.action); // Add actions in reverse order
            }
            current = current.parent;
        }
        System.out.println("PLANNER: Plan reconstructed with " + actions.size() + " actions.");
        return new Plan(actions); // Plan constructor expects a Queue
    }
}
