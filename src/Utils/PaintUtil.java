package Utils;

import org.dreambot.api.utilities.Timer;

import java.awt.*;

public class PaintUtil {  // General utilities for both Quest and Skill paints

    private static final Font font = new Font("Arial", Font.PLAIN, 12);

    public static void drawRuntime(Graphics g, Timer timer, int x, int y) {
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString("Runtime: " + timer.formatTime(), x, y);
    }
}
