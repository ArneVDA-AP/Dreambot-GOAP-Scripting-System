package Core.Actions;

import Core.GOAP.Action;
import Core.GOAP.ActionResult;
import Core.GOAP.WorldState;
import Core.GOAP.WorldStateKey;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.ScriptManager;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action to click a specific WidgetChild based on its ID path.
 */
public class ActionClickWidget implements Action {

    private final int[] widgetPath; // Array of IDs [parentId, childId, grandchildId, ...]
    private final String actionName; // Optional: Specific action to click if widget has multiple
    private final String description; // For logging/naming

    // Optional: State changes anticipated by the planner
    private final Map<WorldStateKey, Object> effectsMap;

    /**
     * Constructor to click a widget with its default action.
     * @param widgetPath The path of IDs to the target WidgetChild.
     * @param description A short description for the action name.
     * @param effects The expected effects on the WorldState.
     */
    public ActionClickWidget(int[] widgetPath, String description, Map<WorldStateKey, Object> effects) {
        this(widgetPath, null, description, effects); // Default action is null (left-click)
    }

    /**
     * Constructor to click a widget with a specific action name.
     * @param widgetPath The path of IDs to the target WidgetChild.
     * @param actionName The specific action text to interact with (if not default left-click).
     * @param description A short description for the action name.
     * @param effects The expected effects on the WorldState.
     */
    public ActionClickWidget(int[] widgetPath, String actionName, String description, Map<WorldStateKey, Object> effects) {
        this.widgetPath = Objects.requireNonNull(widgetPath, "Widget path cannot be null");
        if (widgetPath.length == 0) {
            throw new IllegalArgumentException("Widget path cannot be empty");
        }
        this.actionName = actionName; // Can be null for default interaction
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.effectsMap = (effects != null) ? new HashMap<>(effects) : new HashMap<>();
    }

    @Override
    public String getName() {
        return "ClickWidget_" + description + "_" + Arrays.toString(widgetPath);
    }

    @Override
    public Map<WorldStateKey, Object> getPreconditions() {
        Map<WorldStateKey, Object> preconditions = new HashMap<>();
        // Precondition: Usually requires the parent interface/tab to be open.
        // This should be handled by the Goal or preceding actions ensuring the correct context.
        // Example: preconditions.put(WorldStateKey.UI_EQUIPMENT_TAB_OPEN, true);
        preconditions.put(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN, false); // Cannot usually click widgets during dialogue
        return preconditions;
    }

    @Override
    public Map<WorldStateKey, Object> getEffects() {
        // Return the effects provided during construction
        return effectsMap;
    }

    @Override
    public double getCost() {
        return 0.7; // Clicking widgets is very fast
    }

    @Override
    public boolean isApplicable(WorldState state) {
        if (state.getBoolean(WorldStateKey.INTERACT_IS_DIALOGUE_OPEN)) {
            return false;
        }
        // Check if the widget exists and is visible
        WidgetChild widget = Widgets.get(widgetPath);
        return widget != null && widget.isVisible();
    }

    @Override
    public ActionResult perform(WorldState currentState) {
        WidgetChild widget = Widgets.get(widgetPath);

        if (widget == null || !widget.isVisible()) {
            Logger.log(getName() + ": Target widget not found or not visible. Path: " + Arrays.toString(widgetPath));
            return ActionResult.FAILURE;
        }

        String interaction = (actionName != null) ? actionName : "Click"; // Default to "Click" if no specific action given
        Logger.log(getName() + ": Interacting '" + interaction + "' with widget.");

        if (widget.interact(interaction)) {
            // Clicking widgets is usually fast, but a small sleep can help ensure state changes register
            Sleep.sleep(Calculations.random(300, 600));
            // We assume success if the interact method returns true.
            // Verifying the *result* of the click (e.g., interface changing)
            // is usually the job of the *next* action's preconditions or the observer.
            Logger.log(getName() + ": Interaction successful.");
            currentState.applyEffects(getEffects()); // Apply anticipated effects
            return ActionResult.SUCCESS;
        } else {
            Logger.log(getName() + ": Interaction failed on widget.");
            return ActionResult.FAILURE;
        }
    }
    // No onAbort needed for this simple action
}