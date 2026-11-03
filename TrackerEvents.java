package pl.playertracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/** Alerty (join / leave / wejście w zasięg) + śledzenie gracza na celowniku. */
public final class TrackerEvents {

    private TrackerEvents() {}

    public static final String PREFIX = "§8[§bTracker§8] ";

    private static Set<String> online = null;
    private static Set<String> inRange = new HashSet<>();
    private static Object lastWorld = null;
    private static int ticks = 0;

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            online = null; inRange.clear(); lastWorld = null;
            return;
        }
        if (mc.world != lastWorld) { lastWorld = mc.world; online = null; inRange.clear(); }
        if (++ticks % 10 != 0) return;

        Set<String> now = new HashSet<>();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList())
            now.add(e.getProfile().getName().toLowerCase());
        Set<String> rangeNow = new HashSet<>();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers())
            if (p != mc.player) rangeNow.add(p.getGameProfile().getName().toLowerCase());

        if (online != null && TrackerState.C.alerts) {
            for (String n : TrackerState.C.tracked.keySet()) {
                String l = n.toLowerCase();
                boolean was = online.contains(l), is = now.contains(l);
                if (!was && is) alert(mc, "§a+ §f" + n + " §adołączył do gry", 1.6f);
                else if (was && !is) alert(mc, "§c- §f" + n + " §cwyszedł z gry", 0.8f);
                else if (is && !inRange.contains(l) && rangeNow.contains(l)) {
                    AbstractClientPlayerEntity p = TracerRenderer.find(mc, n);
                    String d = p != null ? " §7(" + (int) p.distanceTo(mc.player) + "m)" : "";
                    alert(mc, "§e! §f" + n + " §ejest w zasięgu" + d, 1.2f);
                }
            }
        }
        online = now;
        inRange = rangeNow;
    }

    private static void alert(MinecraftClient mc, String msg, float pitch) {
        mc.player.sendMessage(Text.literal(PREFIX + msg), false);
        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, pitch);
    }

    /** Dodaje / usuwa gracza, w którego akurat celujesz (tolerancja ok. 5°). */
    public static void toggleLooked(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1f).normalize();
        AbstractClientPlayerEntity best = null;
        double bestDot = 0.995;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            Vec3d to = p.getBoundingBox().getCenter().subtract(eye);
            if (to.length() < 0.5) continue;
            double dot = to.normalize().dotProduct(look);
            if (dot > bestDot) { bestDot = dot; best = p; }
        }
        if (best == null) {
            mc.player.sendMessage(Text.literal(PREFIX + "§7Nie celujesz w żadnego gracza."), true);
            return;
        }
        String name = best.getGameProfile().getName();
        if (TrackerState.C.tracked.containsKey(name)) {
            TrackerState.remove(name);
            mc.player.sendMessage(Text.literal(PREFIX + "§cUsunięto §f" + name), true);
        } else {
            TrackerState.Result r = TrackerState.tryAdd(name);
            mc.player.sendMessage(Text.literal(PREFIX + (r.ok() ? "§a" : "§c") + r.msg()), true);
        }
    }
}
