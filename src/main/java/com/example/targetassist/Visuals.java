package com.example.targetassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Options;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Client-only visual options: fullbright, no fog, no particles, no weather.
@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class Visuals {
    private static boolean nvApplied;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            nvApplied = false;
            return;
        }
        while (Keys.FULLBRIGHT.consumeClick()) {
            if (mc.screen == null) {
                Settings.fullbright = !Settings.fullbright;
                Settings.save();
            }
        }

        // fullbright: a night vision effect that only exists on your own client
        if (Settings.fullbright) {
            MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || (nvApplied && cur.getDuration() < 400)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
                nvApplied = true;
            }
        } else if (nvApplied) {
            mc.player.removeEffectNoUpdate(MobEffects.NIGHT_VISION);
            nvApplied = false;
        }

        // no particles: wipe them every tick, so none are ever drawn
        if (Settings.noParticles) mc.particleEngine.clearParticles();

        applyPerf(mc);
    }

    // FPS mode: fast graphics, no clouds, no entity shadows, no biome blending, no vsync, uncapped fps.
    // Your old settings are remembered and put back when you turn it off.
    private static boolean perfApplied;
    private static GraphicsStatus oldGfx;
    private static CloudStatus oldClouds;
    private static boolean oldShadows, oldVsync;
    private static int oldBlend, oldFps;

    private static void applyPerf(Minecraft mc) {
        if (Settings.perfMode == perfApplied) return;
        Options o = mc.options;
        if (Settings.perfMode) {
            oldGfx = o.graphicsMode().get();
            oldClouds = o.cloudStatus().get();
            oldShadows = o.entityShadows().get();
            oldVsync = o.enableVsync().get();
            oldBlend = o.biomeBlendRadius().get();
            oldFps = o.framerateLimit().get();
            o.graphicsMode().set(GraphicsStatus.FAST);
            o.cloudStatus().set(CloudStatus.OFF);
            o.entityShadows().set(false);
            o.enableVsync().set(false);
            o.biomeBlendRadius().set(0);
            o.framerateLimit().set(260);  // 260 = unlimited
        } else {
            o.graphicsMode().set(oldGfx);
            o.cloudStatus().set(oldClouds);
            o.entityShadows().set(oldShadows);
            o.enableVsync().set(oldVsync);
            o.biomeBlendRadius().set(oldBlend);
            o.framerateLimit().set(oldFps);
        }
        perfApplied = Settings.perfMode;
    }

    // no weather: rain, snow, storm darkening and thunder are all driven by these two values
    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !Settings.noWeather) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.setRainLevel(0f);
        mc.level.setThunderLevel(0f);
    }

    // no fog: pushes the fog start and end so far away that it never shows (covers lava, water, blindness)
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (!Settings.noFog) return;
        e.setNearPlaneDistance(1.0E6F);
        e.setFarPlaneDistance(2.0E6F);
        e.setCanceled(true);
    }

    // no fog also hides the full-screen water and fire overlays
    @SubscribeEvent
    public static void onScreenEffect(RenderBlockScreenEffectEvent e) {
        if (!Settings.noFog) return;
        RenderBlockScreenEffectEvent.OverlayType t = e.getOverlayType();
        if (t == RenderBlockScreenEffectEvent.OverlayType.WATER || t == RenderBlockScreenEffectEvent.OverlayType.FIRE) {
            e.setCanceled(true);
        }
    }
}
