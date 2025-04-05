package Utils;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.script.listener.HumanMouseListener;
import org.dreambot.api.utilities.Timer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;

public class SkillPaintUtil implements HumanMouseListener {

    private static final Font font = new Font("Arial", Font.BOLD, 12);
    private static boolean isVisible = true; // Visibility toggle for the paint
    private static BufferedImage toggleButton; // Button image for toggling paint visibility
    private static Rectangle toggleButtonArea; // Area for the toggle button, to check mouse clicks
    private static final int[] XP_TABLE = { /* XP_TABLE values as provided */ };

    // Skill progression variables
    private static long xpGained;
    private static long timeTillLvl;
    private static int xpPerHr;
    private static int currentXp;
    private int currentLevel;
    private double currentLevelXp;
    private double nextLevelXp;
    private static double percentTNL;
    private int lvlsGained;

    private Timer timer;  // Tracking the time since the start of this node

    public SkillPaintUtil(Timer timer) {
        this.timer = timer;  // Assign the passed-in Timer
    }

    static {
        try {
            toggleButton = ImageIO.read(new URL("https://i.imgur.com/avNCtfS.png"));
            toggleButtonArea = new Rectangle();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPaint(Graphics g, Skill skill) {
        if (!isVisible || g == null || skill == null || timer == null) return;
        // Update current skill data without restarting the SkillTracker
        xpGained = SkillTracker.getGainedExperience(skill);
        xpPerHr = SkillTracker.getGainedExperiencePerHour(skill);
        timeTillLvl = SkillTracker.getTimeToLevel(skill);
        currentXp = Skills.getExperience(skill);
        currentLevel = Skills.getRealLevel(skill);
        currentLevelXp = Skills.getExperienceForLevel(currentLevel);
        nextLevelXp = Skills.getExperienceForLevel(currentLevel + 1);
        percentTNL = ((currentXp - currentLevelXp) / (nextLevelXp - currentLevelXp) * 100);
        lvlsGained = SkillTracker.getGainedLevels(skill);

        int width = 200;
        int height = 150;
        int x = 10; // Adjusted for bottom left
        int y = 190; // Adjust based on client height

        drawSkillInfo(g, skill, x, y, width, height);
    }

    private void drawSkillInfo(Graphics g, Skill skill, int x, int y, int width, int height) {
        if (g == null || skill == null || timer == null) return;

        // Define toggle button bounds
        toggleButtonArea.setBounds(x + width - 20, y, 20, 20);

        // Drawing background and texts
        drawSkillBackground(g, x, y, width, height);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString("Skill: " + skill.getName(), x + 5, y + 15);
        g.drawString("XP Gained: " + xpGained, x + 5, y + 30);
        g.drawString(String.format("XP/Hour: %d", xpPerHr), x + 5, y + 45);
        DecimalFormat df = new DecimalFormat("#.#");
        g.drawString("Percent to Next Lvl: " + df.format(percentTNL) + "%", x + 5, y + 60);

        // Use the formatTime method to format the time to next level
        String formattedTime = formatTime(timeTillLvl);
        g.drawString("Time to Next Lvl: " + formattedTime, x + 5, y + 75);

        g.drawString("Levels gained: " + lvlsGained + " (" + currentLevel + ")", x + 5, y + 90 );

        drawProgressBar(g, x + 5, y + 110, width - 10, percentTNL);

        g.drawString("Runtime: " + timer.formatTime(), x + 5, y + 145);
        // Drawing the toggle button
        g.drawImage(toggleButton, toggleButtonArea.x, toggleButtonArea.y, null);
    }

    private static void drawSkillBackground(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.BLUE);
        g.drawRect(x, y, width, height);
    }

    private static void drawProgressBar(Graphics g, int x, int y, int width, double percent) {
        g.setColor(Color.GREEN);
        int fillWidth = (int) (width * (percent / 100));
        g.fillRect(x, y, fillWidth, 10);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, 10);
    }

    private static String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public String getFormattedTime() {
        return timer.formatTime();
    }

    public long getXPGained() {
        return xpGained;
    }

    public int getXPPerHour() {
        return xpPerHr;
    }

    public double getPercentToNextLevel() {
        return percentTNL;
    }

    public String getTimeToNextLevel() {
        return formatTime(timeTillLvl);
    }

    public int getLevelsGained() {
        return lvlsGained;
    }

    public String getSkillName(Skill skill) {
        return skill.getName();
    }

    //    @Override
//    public void onHumanMouseEvent(HumanMouseEvent e) {
//        if (e.getType() == HumanMouseEvent.Type.CLICKED && toggleButtonArea.contains(e.getPoint())) {
//            isVisible = !isVisible;
//        }
//    }
}
