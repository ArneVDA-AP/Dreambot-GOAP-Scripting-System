package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment; // Import Equipment
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.items.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to equip an item from the inventory.
 */
public class ActionEquipItem implements Action {

    private final String itemName;
    private final WorldStateKey hasItemKey; // Key for having the item in inventory
    private final WorldStateKey isItemEquippedKey; // Key for having the item equipped

    /**
     * Constructor for equipping an item.
     * @param itemName The exact name of the item to equip.
     * @param hasItemKey The WorldStateKey representing possession of the item in inventory.
     * @param isItemEquippedKey The WorldStateKey representing the equipped state of the item.
     */
    public ActionEquipItem(String itemName, WorldStateKey hasItemKey, WorldStateKey isItemEquippedKey) {
        this.itemName = Objects.requireNonNull(itemName);
        this.hasItemKey = Objects.requireNonNull(hasItemKey);
        this.isItemEquippedKey = Objects.requireNonNull(isItemEquippedKey);
    }

    @Override
    public String getName() {
        return "Equip_" + itemName;
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Must have the item in inventory
        preconditions.put(hasItemKey, true);
        // Precondition: Item must not already be equipped
        preconditions.put(isItemEquippedKey, false);
        // Precondition: Not animating (usually safe)
        preconditions.put(WorldStateKey.INTERACT_IS_ANIMATING, false);
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        Map<WorldStateKey, Object> effects = new HashMap<>();
        // Effect: Item is no longer in inventory (usually, unless stackable like arrows)
        // This is hard to guarantee, better to rely on observer update.
        // effects.put(hasItemKey, false);
        // Effect: Item is now equipped
        effects.put(isItemEquippedKey, true);
        return effects;
    }

    @Override
    public double getCost() {
        return 0.9; // Slightly more than simple tab open, less than item-on-item
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check inventory and equipped state
        return state.getBoolean(hasItemKey) && !state.getBoolean(isItemEquippedKey) && !state.getBoolean(WorldStateKey.INTERACT_IS_ANIMATING);
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        // Double check if already equipped (state might be delayed)
        if (Equipment.contains(itemName)) {
            Logger.log(getName() + ": Item already equipped.");
            currentState.setBoolean(isItemEquippedKey, true); // Correct state
            // Also update inventory state if it wasn't stackable
            if (!isStackable(itemName)) { // Need a helper for stackable check or assume non-stackable
                currentState.setBoolean(hasItemKey, false);
            }
            return ActionResult.SUCCESS;
        }

        Item itemToEquip = Inventory.get(itemName);
        if (itemToEquip == null) {
            Logger.log(getName() + ": Item '" + itemName + "' not found in inventory.");
            currentState.setBoolean(hasItemKey, false); // Correct state
            return ActionResult.FAILURE;
        }

        // Determine interaction action ("Wield", "Wear", "Equip")
        String interactionAction = "Wield"; // Default for weapons
        if (itemToEquip.hasAction("Wear")) {
            interactionAction = "Wear";
        } else if (itemToEquip.hasAction("Equip")) {
            interactionAction = "Equip";
        } // Add other actions if needed

        Logger.log(getName() + ": Attempting to '" + interactionAction + "' " + itemName);
        if (itemToEquip.interact(interactionAction)) {
            // Wait for the item to appear in equipment
            boolean equipped = Sleep.sleepUntil(() -> Equipment.contains(itemName), 3000);

            if (equipped) {
                Logger.log(getName() + ": Successfully equipped " + itemName);
                currentState.setBoolean(isItemEquippedKey, true);
                if (!isStackable(itemName)) {
                    currentState.setBoolean(hasItemKey, false);
                }
                return ActionResult.SUCCESS;
            } else {
                Logger.log(getName() + ": Failed to confirm item equipped after interaction.");
                // Check if item is gone from inventory anyway (might indicate equip but API lag)
                if (!Inventory.contains(itemName) && !isStackable(itemName)) {
                    Logger.log(getName() + ": Item removed from inventory, assuming equip succeeded despite confirmation timeout.");
                    currentState.setBoolean(isItemEquippedKey, true);
                    currentState.setBoolean(hasItemKey, false);
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAILURE;
            }
        } else {
            Logger.log(getName() + ": Interaction '" + interactionAction + "' failed on " + itemName);
            return ActionResult.FAILURE;
        }
    }

    // Simple helper - a real implementation might check item definitions
    private boolean isStackable(String name) {
        // Arrows are stackable, most other tutorial items aren't
        return name != null && name.toLowerCase().contains("arrow");
    }

    // No onAbort needed for this simple action
}