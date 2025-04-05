package Utils;

import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.utilities.Timer;

import java.awt.*;
import java.text.DecimalFormat;

public class CombatPaintUtil {

    private static final Font font = new Font("Arial", Font.PLAIN, 10);
    private static final int startY = 50; // Starting Y position for the first skill bar
    private static final int stepY = 70; // Vertical step between skill bars
    private static final int barHeight = 60; // Height of each skill info block
    private static final int barWidth = 220; // Width of the skill info area
    private static final int startX = 10; // Starting X position
    private Timer timer;
    private DecimalFormat df = new DecimalFormat("#.##");

    public CombatPaintUtil() {
        SkillTracker.start(Skill.ATTACK);
        SkillTracker.start(Skill.DEFENCE);
        SkillTracker.start(Skill.STRENGTH);
        SkillTracker.start(Skill.HITPOINTS);
        SkillTracker.start(Skill.PRAYER);
        timer = new Timer();
    }

    public void onPaint(Graphics g) {
        drawRuntime(g, startX, startY - 20); // Display runtime above the skill boxes
        drawSkill(g, Skill.ATTACK, startX, startY, new Color(205, 92, 92), new Color(139, 0, 0)); // Lighter red for Attack text, dark red border
        drawSkill(g, Skill.DEFENCE, startX, startY + stepY, new Color(0, 139, 139), Color.BLUE); // Dark cyan text for Defence, Blue border
        drawSkill(g, Skill.STRENGTH, startX, startY + 2 * stepY, new Color(34, 139, 34), new Color(34, 139, 34)); // Forest green for Strength
        drawSkill(g, Skill.HITPOINTS, startX, startY + 3 * stepY, Color.LIGHT_GRAY, Color.WHITE); // Light gray text for Hitpoints, White border
        drawSkill(g, Skill.PRAYER, startX, startY + 4 * stepY, new Color(255, 215, 0), new Color(218, 165, 32)); // Gold text for Prayer, Goldenrod border
    }

    private void drawRuntime(Graphics g, int x, int y) {
        g.setColor(new Color(0, 0, 0, 150)); // Semi-transparent dark background
        g.fillRect(x, y, barWidth, 20);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, barWidth, 20);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString("Runtime: " + timer.formatTime(), x + 5, y + 15);
    }

    private void drawSkill(Graphics g, Skill skill, int x, int y, Color textColor, Color borderColor) {
        long xpGained = SkillTracker.getGainedExperience(skill);
        int xpPerHr = SkillTracker.getGainedExperiencePerHour(skill);
        long timeTillLvl = SkillTracker.getTimeToLevel(skill);
        int currentXp = Skills.getExperience(skill);
        int currentLevel = Skills.getRealLevel(skill);
        int currentLevelXp = Skills.getExperienceForLevel(currentLevel);
        int nextLevelXp = Skills.getExperienceForLevel(currentLevel + 1);
        double percentTNL = ((currentXp - currentLevelXp) / (double) (nextLevelXp - currentLevelXp) * 100);
        int lvlsGained = SkillTracker.getGainedLevels(skill);

        g.setColor(new Color(0, 0, 0, 150)); // Semi-transparent dark background
        g.fillRect(x, y, barWidth, barHeight);
        g.setColor(borderColor); // Border color based on the skill
        g.drawRect(x, y, barWidth, barHeight);
        g.setFont(font);
        g.setColor(textColor); // Text color based on the skill

        // Drawing the texts
        int textY = y + 14;
        g.drawString(skill.getName(), x + 5, textY);
        g.drawString("XP: " + xpGained + " (" + xpPerHr + " xp/hr)", x + 5, textY + 14);
        g.drawString("Lvl: " + currentLevel + " (" + lvlsGained + ")", x + 5, textY + 28);
        g.drawString("TNL: " + df.format(percentTNL) + "% (" + formatTime(timeTillLvl) + ")", x + 5, textY + 42);
    }

    private String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
