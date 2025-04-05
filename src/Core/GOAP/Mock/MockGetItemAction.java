package Core.GOAP.Mock; // Example package for mock objects

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MockGetItemAction implements Action {

    private final String itemName;
    private final WorldStateKey itemKey; // Key representing if the item is held
    private final String requiredLocation; // Location where the item can be obtained
    private final WorldStateKey locationKey; // Key representing current location

    public MockGetItemAction(String itemName, WorldStateKey itemKey, String requiredLocation, WorldStateKey locationKey) {
        this.itemName = Objects.requireNonNull(itemName);
        this.itemKey = Objects.requireNonNull(itemKey);
        this.requiredLocation = Objects.requireNonNull(requiredLocation);
        this.locationKey = Objects.requireNonNull(locationKey);
    }

    @Override
    public String getName() {
        // Make name slightly more specific if needed, e.g., based on itemKey
        return "MockGetItem_" + itemKey.name();
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        // Precondition: Must be in the required location (unless location is "Anywhere")
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        if (!"Anywhere".equalsIgnoreCase(requiredLocation)) {
            preconditions.put(locationKey, requiredLocation);
        }
        // Precondition: Don't already have the item
        preconditions.put(itemKey, false); // Planner uses this
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Effect: Player now has the item
        Map<WorldStateKey, Object> effects = new HashMap<>();
        effects.put(itemKey, true);
        return effects;
    }

    @Override
    public double getCost() {
        return 1.0;
    }

    @Override
    public boolean isApplicable(WorldState state) {
        // Check location (if specific location required) and if item already possessed
        boolean locationOk = "Anywhere".equalsIgnoreCase(requiredLocation) ||
                requiredLocation.equals(state.getString(locationKey));
        boolean alreadyHasItem = state.getBoolean(itemKey);
        return locationOk && !alreadyHasItem;
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        // Mock action succeeds instantly
        System.out.println("SIM: Performing " + getName());
        // In a real action, you'd use GroundItems.closest(itemName).interact("Take")
        // and check Inventory.contains(itemName), possibly returning IN_PROGRESS
        return ActionResult.SUCCESS;
    }
}