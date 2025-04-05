package Utils;

import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.script.TaskNode;

import java.awt.*;

public class WalkerNode extends TaskNode implements Paintable {

    public enum Destination {
        GE(new Area(3161, 3487, 3169, 3494), "Grand Exchange", "Resources/Bank_Icon.png"),
        GERRANTS_FISHY_BUSINESS(new Area(3010, 3224, 3015, 3228), "Gerrant's Fishy Business", "Resources/FishingShop_Icon.png"),
        DRAYNOR_BANK(new Area(3092, 3249, 3096, 3241), "Draynor Bank", "Resources/Bank_Icon.png"),
        FALADOR_EAST_BANK(new Area(3011, 3360, 3018, 3356), "Falador East Bank", "Resources/Bank_Icon.png"),
        FALADOR_WEST_BANK(new Area(2943, 3375, 2947, 3370), "Falador West Bank", "Resources/Bank_Icon.png"),
        LUMBRIDGE_YARD(new Area(3217, 3229, 3225, 3207), "Lumbridge Yard", "Resources/Town_Icon.png"),
        AL_KHARID_BANK(new Area(3269, 3172, 3277, 3162), "Al Kharid Bank", "Resources/Bank_Icon.png"),
        VARROCK_EAST_BANK(new Area(3251, 3428, 3256, 3420), "Varrock East Bank", "Resources/Bank_Icon.png"),
        VARROCK_WEST_BANK(new Area(3180, 3450, 3189, 3429), "Varrock West Bank", "Resources/Bank_Icon.png"),
        EDGEVILLE_BANK(new Area(3087, 3503, 3093, 3489), "Edgeville Bank", "Resources/Bank_Icon.png");

        private final Area area;
        private final String displayName;
        private final String iconPath;

        Destination(Area area, String displayName, String iconPath) {
            this.area = area;
            this.displayName = displayName;
            this.iconPath = iconPath;
        }

        public Area getArea() {
            return area;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconPath() {
            return iconPath;
        }
    }

    private final Destination destination;

    public WalkerNode(Destination destination) {
        this.destination = destination;
    }

    @Override
    public boolean accept() {
        return !destination.getArea().contains(Players.getLocal());
    }

    @Override
    public int execute() {
        ScriptUtils.walkToArea(destination.getArea());
        return 600; // Time in milliseconds to wait before the next loop
    }

    @Override
    public String toString() {
        return destination.getDisplayName();
    }

    @Override
    public void onPaint(Graphics2D g) {
        g.drawString("Walking to: " + destination.getDisplayName(), 10, 40);
    }
}
