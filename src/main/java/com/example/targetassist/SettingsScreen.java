package com.example.targetassist;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class SettingsScreen extends Screen {
    private static final int DIM = 0xA0000000, BORDER = 0xFF343B45, PANEL = 0xFF14181D, HEADER = 0xFF1B2128;
    private static final int GOLD = 0xFFF0B429, TEXT = 0xFFE8ECF1, MUTED = 0xFF8B96A3;
    private static final int ON = 0xFF3DDC84, OFF = 0xFF3A424C, HOVER = 0x18FFFFFF, TRACK = 0xFF2A313A;
    private static final int COL_W = 140, GAP = 10, PAD = 12, TITLE_H = 24, TOP = 6, FOOT_H = 28;

    private final List<Item> items = new ArrayList<>();
    private int px, py, pw, ph, doneX, doneY;
    private Item dragging;
    private KeyItem listening;

    public SettingsScreen() {
        super(Component.literal("Minecraft Settings"));
    }

    @Override
    protected void init() {
        items.clear();
        int[] cy = {0, 0, 0};

        // left column: display + targets
        add(0, cy, new Header("Display"));
        add(0, cy, new Toggle("Health indicators", () -> Settings.healthIndicators, v -> Settings.healthIndicators = v));
        add(0, cy, new Toggle("Player sight", () -> Settings.playerSight, v -> Settings.playerSight = v));
        add(0, cy, new Toggle("Invisible markers", () -> Settings.invisMarkers, v -> Settings.invisMarkers = v));
        add(0, cy, new Toggle("Tab highlight", () -> Settings.tabHighlight, v -> Settings.tabHighlight = v));
        add(0, cy, new Toggle("Side list", () -> Settings.nearbyList, v -> Settings.nearbyList = v));
        add(0, cy, new Slide("Nearby range", 16, 256, "m", () -> Settings.nearbyRange, v -> Settings.nearbyRange = v));
        add(1, cy, new Header("Targets"));
        add(1, cy, new Toggle("Players", () -> Settings.tPlayers, v -> Settings.tPlayers = v));
        add(1, cy, new Toggle("Zombies", () -> Settings.tZombies, v -> Settings.tZombies = v));
        add(1, cy, new Toggle("Other hostile", () -> Settings.tHostile, v -> Settings.tHostile = v));
        add(1, cy, new Toggle("Passive mobs", () -> Settings.tPassive, v -> Settings.tPassive = v));

        // right column: aim assist + keybinds
        add(1, cy, new Header("Aim assist"));
        add(1, cy, new Toggle("Lock-on", () -> Settings.lockOn, v -> Settings.lockOn = v));
        add(1, cy, new Choice("Aim at", () -> Settings.aimHead ? "Head" : "Chest", () -> Settings.aimHead = !Settings.aimHead));
        add(1, cy, new Choice("Priority", () -> Settings.closestToMe ? "Closest to me" : "Crosshair", () -> Settings.closestToMe = !Settings.closestToMe));
        add(1, cy, new Slide("Lock range", 10, 300, "m", () -> Settings.lockRange, v -> Settings.lockRange = v));
        add(1, cy, new Slide("Lock speed", 30, 720, " deg/s", () -> Settings.lockSpeed, v -> Settings.lockSpeed = v));
        add(0, cy, new Header("Keybinds"));
        add(0, cy, new KeyItem("Open menu", Keys.OPEN_GUI));
        add(0, cy, new KeyItem("Lock key", Keys.LOCK_ON));
        add(0, cy, new KeyItem("Freecam key", Keys.FREECAM));
        add(0, cy, new KeyItem("Fullbright key", Keys.FULLBRIGHT));

        // third column: radar
        add(2, cy, new Header("Radar"));
        add(2, cy, new Toggle("Radar", () -> Settings.radarOn, v -> Settings.radarOn = v));
        add(2, cy, new Toggle("Players", () -> Settings.radarPlayers, v -> Settings.radarPlayers = v));
        add(2, cy, new Toggle("Hostile mobs", () -> Settings.radarHostile, v -> Settings.radarHostile = v));
        add(2, cy, new Toggle("Passive mobs", () -> Settings.radarPassive, v -> Settings.radarPassive = v));
        add(2, cy, new Slide("Radar range", 16, 200, "m", () -> Settings.radarRange, v -> Settings.radarRange = v));

        add(2, cy, new Header("Visuals"));
        add(2, cy, new Toggle("Fullbright", () -> Settings.fullbright, v -> Settings.fullbright = v));
        add(2, cy, new Toggle("No fog", () -> Settings.noFog, v -> Settings.noFog = v));
        add(2, cy, new Toggle("No particles", () -> Settings.noParticles, v -> Settings.noParticles = v));
        add(2, cy, new Toggle("No weather", () -> Settings.noWeather, v -> Settings.noWeather = v));
        add(2, cy, new Toggle("FPS mode", () -> Settings.perfMode, v -> Settings.perfMode = v));
        add(2, cy, new Slide("Freecam speed", 2, 100, " b/s", () -> Settings.freecamSpeed, v -> Settings.freecamSpeed = v));

        pw = COL_W * 3 + GAP * 2 + PAD * 2;
        ph = TITLE_H + TOP + Math.max(cy[0], Math.max(cy[1], cy[2])) + FOOT_H;
        px = (width - pw) / 2;
        py = Math.max(2, (height - ph) / 2);
        for (Item it : items) {
            it.x += px + PAD;
            it.y += py + TITLE_H + TOP;
        }
        doneX = px + pw / 2 - 50;
        doneY = py + ph - FOOT_H + 4;
    }

    private void add(int col, int[] cy, Item it) {
        it.x = col * (COL_W + GAP);
        it.y = cy[col];
        it.w = COL_W;
        cy[col] += it.h + 1;
        items.add(it);
    }

    private boolean overDone(double mx, double my) {
        return mx >= doneX && mx < doneX + 100 && my >= doneY && my < doneY + 18;
    }

    private void finishListening() {
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = null;
    }

    // ---------- drawing ----------
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, DIM);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px, py, px + pw, py + TITLE_H, HEADER);
        g.fill(px, py + TITLE_H, px + pw, py + TITLE_H + 1, GOLD);
        g.drawString(font, "Minecraft Settings", px + PAD, py + 8, GOLD, false);
        String hint = Keys.OPEN_GUI.getTranslatedKeyMessage().getString() + " to close";
        g.drawString(font, hint, px + pw - PAD - font.width(hint), py + 8, MUTED, false);

        for (Item it : items) it.render(g, mx, my);

        boolean over = overDone(mx, my);
        g.fill(doneX, doneY, doneX + 100, doneY + 18, over ? 0xFFFFC94D : GOLD);
        g.drawCenteredString(font, "Done", doneX + 50, doneY + 5, 0xFF14181D);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        Settings.save();
    }

    // ---------- input ----------
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) listening.km.setKey(InputConstants.getKey(key, scan));
            finishListening();
            return true;
        }
        if (Keys.OPEN_GUI.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) {
            listening.km.setKey(InputConstants.Type.MOUSE.getOrCreate(button));
            finishListening();
            return true;
        }
        if (button == 0) {
            if (overDone(mx, my)) {
                onClose();
                return true;
            }
            for (Item it : items) {
                if (it.hit(mx, my)) {
                    it.click(mx, my);
                    if (it instanceof Slide) dragging = it;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.drag(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    // ---------- custom widgets ----------
    private abstract class Item {
        int x, y, w, h = 15;
        final String label;

        Item(String label) {
            this.label = label;
        }

        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void hover(GuiGraphics g, int mx, int my) {
            if (hit(mx, my)) g.fill(x, y, x + w, y + h, HOVER);
        }

        abstract void render(GuiGraphics g, int mx, int my);

        void click(double mx, double my) {
        }

        void drag(double mx) {
        }
    }

    private class Header extends Item {
        Header(String label) {
            super(label);
            h = 17;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            g.drawString(font, label.toUpperCase(), x, y + 3, GOLD, false);
            g.fill(x, y + 13, x + w, y + 14, BORDER);
        }
    }

    private class Toggle extends Item {
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;

        Toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(label);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            boolean on = getter.getAsBoolean();
            int sx = x + w - 28, sy = y + 3;
            g.fill(sx, sy, sx + 24, sy + 9, on ? ON : OFF);
            int kx = on ? sx + 16 : sx + 1;
            g.fill(kx, sy + 1, kx + 7, sy + 8, 0xFFFFFFFF);
        }

        @Override
        void click(double mx, double my) {
            setter.accept(!getter.getAsBoolean());
        }
    }

    private class Choice extends Item {
        final Supplier<String> value;
        final Runnable action;

        Choice(String label, Supplier<String> value, Runnable action) {
            super(label);
            this.value = value;
            this.action = action;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            String v = value.get();
            int bw = font.width(v) + 12;
            int bx = x + w - bw - 2;
            g.fill(bx, y + 1, bx + bw, y + h - 1, TRACK);
            g.drawString(font, v, bx + 6, y + 3, GOLD, false);
        }

        @Override
        void click(double mx, double my) {
            action.run();
        }
    }

    private class Slide extends Item {
        final double min, max;
        final String unit;
        final DoubleSupplier getter;
        final DoubleConsumer setter;

        Slide(String label, double min, double max, String unit, DoubleSupplier getter, DoubleConsumer setter) {
            super(label);
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.getter = getter;
            this.setter = setter;
            h = 24;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 2, TEXT, false);
            String v = Math.round(getter.getAsDouble()) + unit;
            g.drawString(font, v, x + w - 4 - font.width(v), y + 2, GOLD, false);
            int tx = x + 4, tw = w - 8, ty = y + 16;
            double f = Mth.clamp((getter.getAsDouble() - min) / (max - min), 0.0, 1.0);
            int kx = tx + (int) (tw * f);
            g.fill(tx, ty, tx + tw, ty + 3, TRACK);
            g.fill(tx, ty, kx, ty + 3, GOLD);
            g.fill(kx - 2, ty - 3, kx + 2, ty + 6, 0xFFFFFFFF);
        }

        @Override
        void click(double mx, double my) {
            drag(mx);
        }

        @Override
        void drag(double mx) {
            double f = Mth.clamp((mx - (x + 4)) / (w - 8), 0.0, 1.0);
            setter.accept(Math.round(min + f * (max - min)));
        }
    }

    private class KeyItem extends Item {
        final KeyMapping km;

        KeyItem(String label, KeyMapping km) {
            super(label);
            this.km = km;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            boolean wait = listening == this;
            String v = wait ? "press a key..." : km.getTranslatedKeyMessage().getString();
            int bw = Math.max(44, font.width(v) + 12);
            int bx = x + w - bw - 2;
            g.fill(bx - 1, y, bx + bw + 1, y + h, wait ? GOLD : BORDER);
            g.fill(bx, y + 1, bx + bw, y + h - 1, TRACK);
            g.drawString(font, v, bx + (bw - font.width(v)) / 2, y + 3, wait ? GOLD : TEXT, false);
        }

        @Override
        void click(double mx, double my) {
            listening = this;
        }
    }
}
