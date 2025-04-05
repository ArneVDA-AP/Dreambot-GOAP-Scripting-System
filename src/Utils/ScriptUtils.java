package Utils;

import org.dreambot.api.Client;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;

import static org.dreambot.api.utilities.Logger.log;
import static org.dreambot.api.utilities.Sleep.sleep;

public class ScriptUtils {

    public static boolean handleLevelUpDialogue() {
        boolean handled = false;
        Sleep.sleep(Calculations.random(1800, 6900));
        while (Dialogues.inDialogue()) {
            Dialogues.continueDialogue();
            sleep(Calculations.random(400, 600));
            handled = true;  // Set to true because we are handling dialogue
        }
        return handled;  // Return true if any dialogue was handled, false otherwise
    }

    public static boolean walkToArea(Area area) {
        log("Walking to " + area);
        while (!area.contains(Players.getLocal())) {
            Walking.walk(area);
            if (Calculations.random(100) > 60) {
                Mouse.moveOutsideScreen(true);
            }
            boolean reached = Sleep.sleepUntil(() -> area.contains(Players.getLocal()), Calculations.random(1200, 5200));
            if (!reached) {
                if (Calculations.random(100) < 7) {
                    Sleep.sleep(Calculations.random(4000, 33000));
                }
            }
        }
        return true;
    }

    public static void continueDialogue() {
        while (Dialogues.canContinue()) {
            Dialogues.continueDialogue();
            sleep(Calculations.random(600, 800));
        }
    }

    public static void handleDialogueWithOptions(int option) {
        while (Dialogues.inDialogue()) {
            if (Dialogues.canContinue()) {
                continueDialogue();
            } else if (Dialogues.areOptionsAvailable()) {
                Dialogues.chooseOption(option);
            }
            Sleep.sleep(Calculations.random(450, 730));
        }
    }


    public void waitForCutsceneAndDialogue() {
        while (Client.isInCutscene() || Dialogues.inDialogue()) {
            if (Client.isInCutscene()) {
                log("Cutscene detected, waiting for it to end.");
                Sleep.sleepUntil(() -> !Client.isInCutscene(), 120000); // Wait up to 120 seconds for cutscene to end
            }
            if (Dialogues.inDialogue()) {
                log("Continuing dialogue after cutscene.");
                while (Dialogues.canContinue() || Dialogues.areOptionsAvailable()) {
                   continueDialogue();
                }
            }
        }
    }
}