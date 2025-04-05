package Utils;
/**
 * SimpleChickenFighter.WindMouse from SMART by Benland100
 * Copyright to Benland100, (Benjamin J. Land)
 * Originally modified for use with DreamBot 3 by holic
 * Mouse dragging issues fixed by Hero with help from Pandemic
 * <p>
 * Prepped for DreamBot 3
 **/

import org.dreambot.api.input.Mouse;
import org.dreambot.api.input.event.impl.mouse.MouseButton;
import org.dreambot.api.input.mouse.algorithm.MouseAlgorithm;
import org.dreambot.api.input.mouse.destination.AbstractMouseDestination;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.ViewportTools;
import org.dreambot.api.methods.input.mouse.MouseSettings;
import org.dreambot.api.utilities.Logger;
import org.dreambot.core.Instance;

import java.awt.*;
import java.awt.event.MouseEvent;

public class WindMouse implements MouseAlgorithm { // https://dreambot.org/forums/index.php?/topic/21147-windmouse-custom-mouse-movement-algorithm/
    private int _mouseSpeed = MouseSettings.getSpeed() > Calculations.random(10, 17) ? MouseSettings.getSpeed() - Calculations.random(8, 12) : Calculations.random(8, 17);
    private final int _mouseSpeedLow = Math.round((float) _mouseSpeed / Calculations.random(1, 3));
    private int _mouseGravity = Calculations.random(3, 27);
    private int _mouseWind = Calculations.random(1, 14);


    @Override
    public boolean handleMovement(AbstractMouseDestination abstractMouseDestination) {
        //Get a suitable point for the mouse's destination
        Point suitPos = abstractMouseDestination.getSuitablePoint();

        // Select which implementation of SimpleChickenFighter.WindMouse you'd like to use
        // by uncommenting out the line you want to use below:

        //windMouse(suitPos.x, suitPos.y); //Original implementation
        windMouse2(suitPos); //Tweaked implementation

        return distance(Mouse.getPosition(), suitPos) < 2;
    }

    @Override
    public boolean handleClick(MouseButton mouseButton) {
        return Mouse.getDefaultMouseAlgorithm().handleClick(mouseButton);
    }

    public static void sleep(int min, int max) {
        try {
            Thread.sleep(Calculations.random(min, max));
        } catch (InterruptedException e) {
            Logger.log(e.getMessage());
        }
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Logger.log(e.getMessage());
        }
    }

    /**
     * Tweaked implementation of SimpleChickenFighter.WindMouse
     * Moves to a mid point on longer moves to seem a little more human-like
     * Remove the if statement below if you'd rather straighter movement
     *
     * @param point The destination point
     */
    public void windMouse2(Point point) {
        Point curPos = Mouse.getPosition();
        if (distance(point, curPos) > 250 && Calculations.random(1) == 2) {
            Point rp = randomPoint(point, curPos);
            windMouse2(curPos.x, curPos.y, rp.x, rp.y, _mouseGravity, _mouseWind, _mouseSpeed, Calculations.random(5, 25));
            sleep(1, 300);
        }
        windMouse2(curPos.x, curPos.y, point.x, point.y, _mouseGravity, _mouseWind, _mouseSpeed, Calculations.random(5, 25));
        _mouseGravity = Calculations.random(3, 27);
        _mouseWind = Calculations.random(1, 14);
        _mouseSpeed = Calculations.random(_mouseSpeedLow, MouseSettings.getSpeed());
    }

    /**
     * Tweaked implementation of SimpleChickenFighter.WindMouse by holic
     * All credit to Benjamin J. Land for the original. (see below)
     *
     * @param xs         The x start
     * @param ys         The y start
     * @param xe         The x destination
     * @param ye         The y destination
     * @param gravity    Strength pulling the position towards the destination
     * @param wind       Strength pulling the position in random directions
     * @param targetArea Radius of area around the destination that should
     *                   trigger slowing, prevents spiraling
     */
    private void windMouse2(double xs, double ys, double xe, double ye, double gravity, double wind, double speed, double targetArea) {

        double dist, veloX = 0, veloY = 0, windX = 0, windY = 0;

        double sqrt2 = Math.sqrt(2);
        double sqrt3 = Math.sqrt(3);
        double sqrt5 = Math.sqrt(5);

        int tDist = (int) distance(xs, ys, xe, ye);
        long t = System.currentTimeMillis() + Calculations.random(8000, 13000);

        while (!(Math.hypot((xs - xe), (ys - ye)) < 1)) {
            if (System.currentTimeMillis() > t) break;

            dist = Math.hypot((xs - xe), (ys - ye));
            wind = Math.min(wind, dist);
            if ((dist < 1)) {
                dist = 1;
            }

            long d = (Math.round((Math.round(((double) (tDist))) * 0.3)) / 7);
            if ((d > 25)) {
                d = 25;
            }

            if ((d < 5)) {
                d = 5;
            }

            double rCnc = Calculations.random(6);
            if ((rCnc == 1)) {
                d = 2;
            }

            double maxStep = (Math.min(d, Math.round(dist))) * 1.5;
            if ((dist >= targetArea)) {
                windX = (windX / sqrt3) + ((Calculations.random((int) ((Math.round(wind) * 2) + 1)) - wind) / sqrt5);
                windY = (windY / sqrt3) + ((Calculations.random((int) ((Math.round(wind) * 2) + 1)) - wind) / sqrt5);
            } else {
                windX = (windX / sqrt2);
                windY = (windY / sqrt2);
            }

            veloX += windX + gravity * (xe - xs) / dist;
            veloY += windY + gravity * (ye - ys) / dist;

            if ((Math.hypot(veloX, veloY) > maxStep)) {
                maxStep = ((maxStep / 2) < 1) ? 2 : maxStep;

                double randomDist = (maxStep / 2) + Calculations.random((int) (Math.round(maxStep) / 2));
                double veloMag = Math.sqrt(((veloX * veloX) + (veloY * veloY)));
                veloX = (veloX / veloMag) * randomDist;
                veloY = (veloY / veloMag) * randomDist;
            }

            int lastX = ((int) (Math.round(xs)));
            int lastY = ((int) (Math.round(ys)));
            xs += veloX;
            ys += veloY;
            if ((lastX != Math.round(xs)) || (lastY != Math.round(ys))) {
                setMousePosition(new Point((int) Math.round(xs), (int) Math.round(ys)));
            }

            int w = Calculations.random((int) (Math.round(100 / speed))) * 6;
            if ((w < 5)) {
                w = 5;
            }

            w = (int) Math.round(w * 0.9);
            sleep(w);
        }

        if (((Math.round(xe) != Math.round(xs)) || (Math.round(ye) != Math.round(ys)))) {
            setMousePosition(new Point((int) Math.round(xs), (int) Math.round(ys)));
        }
    }

    /**
     * Internal mouse movement algorithm from SMART. Do not use this without credit to either
     * Benjamin J. Land or BenLand100. This was originally synchronized to prevent multiple
     * motions and bannage but functions poorly with DB3.
     * <p>
     * BEST USED IN FIXED MODE
     *
     * @param xs         The x start
     * @param ys         The y start
     * @param xe         The x destination
     * @param ye         The y destination
     * @param gravity    Strength pulling the position towards the destination
     * @param wind       Strength pulling the position in random directions
     * @param minWait    Minimum relative time per step
     * @param maxWait    Maximum relative time per step
     * @param maxStep    Maximum size of a step, prevents out of control motion
     * @param targetArea Radius of area around the destination that should
     *                   trigger slowing, prevents spiraling
     * @result The actual end point
     */
    private Point windMouseImpl(double xs, double ys, double xe, double ye, double gravity, double wind, double minWait, double maxWait, double maxStep, double targetArea) {
        final double sqrt3 = Math.sqrt(3);
        final double sqrt5 = Math.sqrt(5);

        double dist, veloX = 0, veloY = 0, windX = 0, windY = 0;
        while ((dist = Math.hypot(xs - xe, ys - ye)) >= 1) {
            wind = Math.min(wind, dist);
            if (dist >= targetArea) {
                windX = windX / sqrt3 + (2D * Math.random() - 1D) * wind / sqrt5;
                windY = windY / sqrt3 + (2D * Math.random() - 1D) * wind / sqrt5;
            } else {
                windX /= sqrt3;
                windY /= sqrt3;
                if (maxStep < 3) {
                    maxStep = Math.random() * 3D + 3D;
                } else {
                    maxStep /= sqrt5;
                }
            }
            veloX += windX + gravity * (xe - xs) / dist;
            veloY += windY + gravity * (ye - ys) / dist;
            double veloMag = Math.hypot(veloX, veloY);
            if (veloMag > maxStep) {
                double randomDist = maxStep / 2D + Math.random() * maxStep / 2D;
                veloX = (veloX / veloMag) * randomDist;
                veloY = (veloY / veloMag) * randomDist;
            }
            int lastX = ((int) (Math.round(xs)));
            int lastY = ((int) (Math.round(ys)));
            xs += veloX;
            ys += veloY;
            if ((lastX != Math.round(xs)) || (lastY != Math.round(ys))) {
                setMousePosition(new Point((int) Math.round(xs), (int) Math.round(ys)));
            }
            double step = Math.hypot(xs - lastX, ys - lastY);
            sleep((int) Math.round((maxWait - minWait) * (step / maxStep) + minWait));
        }
        return new Point((int) xs, (int) ys);
    }

    /**
     * Moves the mouse from the current position to the specified position.
     * Approximates human movement in a way where smoothness and accuracy are
     * relative to speed, as it should be.
     *
     * @param x The x destination
     * @param y The y destination
     * @result The actual end point
     */
    public Point windMouse(int x, int y) {
        Point c = Mouse.getPosition();
        double speed = (Math.random() * 15D + 15D) / 10D;
        return windMouseImpl(c.x, c.y, x, y, 9D, 3D, 5D / speed, 10D / speed, 10D * speed, 8D * speed);
    }

    private void setMousePosition(Point endPoint) {
        if (Mouse.getMouseSettings().isDrag()) { // pan told me to do this for dragging. more info in the DreamBot EDU server: https://discord.com/channels/426346091173380096/462764560772759563/1125126497343123566
            Instance.dispatchCanvasEvent(new MouseEvent(Instance.getCanvas(),
                    MouseEvent.MOUSE_DRAGGED,
                    System.currentTimeMillis(),
                    0,
                    endPoint.x,
                    endPoint.y,
                    ViewportTools.getAbsoluteXCoordinate() + endPoint.x,
                    ViewportTools.getAbsoluteYCoordinate() + endPoint.y,
                    0,
                    false,
                    MouseEvent.BUTTON2));
        } else {
            Mouse.hop(endPoint);
        }
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((Math.pow((Math.round(x2) - Math.round(x1)), 2) + Math.pow((Math.round(y2) - Math.round(y1)), 2)));
    }

    public double distance(Point p1, Point p2) {
        return Math.sqrt((p2.y - p1.y) * (p2.y - p1.y) + (p2.x - p1.x) * (p2.x - p1.x));
    }

    public static float randomPointBetween(float corner1, float corner2) {
        if (corner1 == corner2) {
            return corner1;
        }
        float delta = corner2 - corner1;
        float offset = Calculations.getRandom().nextFloat() * delta;
        return corner1 + offset;
    }

    public Point randomPoint(Point p1, Point p2) {
        int randomX = (int) randomPointBetween(p1.x, p2.x);
        int randomY = (int) randomPointBetween(p1.y, p2.y);
        return new Point(randomX, randomY);
    }

}