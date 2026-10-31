package com.example.targetassist;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

// Client-only freecam: a local fake entity is used as the camera. Nothing is sent to the server,
// and your real player stands still (no movement, no attacking or using items) while it is on.
@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class Freecam {
    private static boolean active;
    private static RemotePlayer cam;
    private static double x, y, z;
    private static float camYaw, camPitch, frozenYaw, frozenPitch;
    private static CameraType oldView;
    private static long lastNanos;

    private static void start(Minecraft mc) {
        LocalPlayer p = mc.player;
        cam = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "freecam"));
        x = p.getX();
        y = p.getY();
        z = p.getZ();
        camYaw = frozenYaw = p.getYRot();
        camPitch = frozenPitch = p.getXRot();
        cam.setPos(x, y, z);
        cam.xo = x;
        cam.yo = y;
        cam.zo = z;
        oldView = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(cam);
        lastNanos = System.nanoTime();
        active = true;
    }

    private static void stop(Minecraft mc) {
        active = false;
        cam = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (oldView != null) mc.options.setCameraType(oldView);
    }

    // ---------- per tick: toggle key, block attacking/using ----------
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickEvent.Phase.START) {
            if (active) {
                mc.options.keyAttack.setDown(false);
                mc.options.keyUse.setDown(false);
                Keys.LOCK_ON.setDown(false);
            }
            return;
        }
        if (mc.player == null || mc.level == null) {
            active = false;
            cam = null;
            return;
        }
        while (Keys.FREECAM.consumeClick()) {
            if (mc.screen != null) continue;
            if (active) stop(mc);
            else start(mc);
        }
        // respawn or dimension change resets the camera, so drop out of freecam
        if (active && mc.getCameraEntity() != cam) {
            active = false;
            cam = null;
        }
    }

    // ---------- per frame: fly the camera ----------
    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !active) return;
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.1);
        lastNanos = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cam == null || mc.screen != null) return;
        Keys.LOCK_ON.setDown(false);

        double speed = Settings.freecamSpeed * (mc.options.keySprint.isDown() ? 2.5 : 1.0);
        double yaw = Math.toRadians(camYaw), pitch = Math.toRadians(camPitch);
        double fwd = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double side = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
        double vert = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);

        double dx = fwd * -Math.sin(yaw) * Math.cos(pitch) + side * -Math.cos(yaw);
        double dy = fwd * -Math.sin(pitch) + vert;
        double dz = fwd * Math.cos(yaw) * Math.cos(pitch) + side * -Math.sin(yaw);
        x += dx * speed * dt;
        y += dy * speed * dt;
        z += dz * speed * dt;
        cam.setPos(x, y, z);
        cam.xo = x;
        cam.yo = y;
        cam.zo = z;
    }

    // mouse movement turns the real player, so move that turn over to the camera and put the player back
    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!active) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        camYaw += Mth.wrapDegrees(p.getYRot() - frozenYaw);
        camPitch = Mth.clamp(camPitch + (p.getXRot() - frozenPitch), -90f, 90f);
        p.setYRot(frozenYaw);
        p.setXRot(frozenPitch);
        p.yRotO = frozenYaw;
        p.xRotO = frozenPitch;
        e.setYaw(camYaw);
        e.setPitch(camPitch);
    }

    // ---------- freeze the real player ----------
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!active) return;
        Input in = e.getInput();
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!active) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (active) e.setCanceled(true);  // hide the first-person hand
    }
}
