package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.tabs.Tab; // Import Tab enum
import org.dreambot.api.methods.tabs.Tabs; // Import Tabs class
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to open a specific interface Tab (Inventory, Skills, Quest, etc.).
 */
public class ActionOpenTab implements Action {

    private final Tab targetTab;
    private final WorldStateKey tabOpenKey; // The key representing this tab's open state

    /**
     * Constructor for opening a tab.
     * @param targetTab The specific Tab enum constant to open.
     * @param tabOpenKey The WorldStateKey representing the open state of this tab.
     */
    public ActionOpenTab(Tab targetTab, WorldStateKey tabOpenKey) {
        this.targetTab = Objects.requireNonNull(targetTab, "Target Tab cannot be null");
        this.tabOpenKey = Objects.requireNonNull(tabOpenKey, "Tab Open Key cannot be null");
    }

    @Override
    public String getName() {
        return "OpenTab_" + targetTab.name();
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: The target tab should not already be open
        preconditions.put(tabOpenKey, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: The target tab is now open
        effects.put(tabOpenKey, true);
        // Opening one tab might implicitly close others, but we won't model that complexity here.
        return effects;
    }

    @Override
    public double getCost() {
        return 0.8; // Very quick action
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Applicable only if the tab is not currently open
        return !state.getBoolean(tabOpenKey);
        // We could also use Tabs.isOpen(targetTab) for a direct runtime check,
        // but relying on the WorldState is the standard GOAP way.
        // return !Tabs.isOpen(targetTab);
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        // Double-check if already open (state might be slightly delayed)
        if (Tabs.isOpen(targetTab)) {
            Logger.log(getName() + ": Tab already open.");
            currentState.setBoolean(tabOpenKey, true); // Correct state if needed
            return ActionResult.SUCCESS;
        }

        Logger.log(getName() + ": Attempting to open " + targetTab.name() + " tab.");
        if (Tabs.open(targetTab)) {
            // Wait briefly for the tab to visually open
            boolean opened = Sleep.sleepUntil(() -> Tabs.isOpen(targetTab), 2000);
            if (opened) {
                Logger.log(getName() + ": Tab opened successfully.");
                currentState.setBoolean(tabOpenKey, true); // Update state
                return ActionResult.SUCCESS;
            } else {
                Logger.log(getName() + ": Failed to confirm tab opened after interaction.");
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Tabs.open() command failed for " + targetTab.name());
            return ActionResult.FAILURE;
        }
    }
    // No onAbort needed for this simple action
}