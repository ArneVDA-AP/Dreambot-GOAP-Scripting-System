package Utils;

import org.dreambot.api.utilities.Timer;

import java.awt.*;

public class QuestPaintUtil {

    public static void drawQuestBackground(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.YELLOW);
        g.drawRect(x, y, width, height);
    }

    public static void drawQuestInfo(Graphics g, String questName, String stage, Timer timer, int x, int y) {
        PaintUtil.drawRuntime(g, timer, x, y);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        g.drawString("Quest: " + questName, x, y + 15);
        g.drawString("Stage: " + stage, x, y + 30);
    }
}
