package pl.playertracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrackerScreen extends Screen {

    private enum Tab {
        PLAYERS("Gracze"), ONLINE("Online"), TRACERS("Tracery"), SETTINGS("Ustawienia");
        final String label;
        Tab(String l) { label = l; }
    }

    // kolory interfejsu (RGB)
    private static final int ACCENT = 0x00B7FF, GREEN = 0x3CDC64, RED = 0xFF4D4D, GRAY = 0x8A97A3, GOLD = 0xFFB020;

    private static final int PW = 340, PH_MAX = 316, ROW = 19;
    private static Tab lastTab = Tab.PLAYERS;
    private static String keep = "";

    private Tab tab = lastTab;
    private int px, py, ph;
    private TextFieldWidget field;
    private int scroll = 0;
    private boolean dirty = false;
    private String msg = "";
    private int msgColor = 0xFFFFFF;
    private long msgTime = 0;

    // dane aktualnie widocznej listy (do rysowania)
    private List<String> rowNames = new ArrayList<>();
    private List<PlayerListEntry> rowEntries = new ArrayList<>();
    private int listY, visRows, totalRows;

    public TrackerScreen() { super(Text.literal("Player Tracker")); }

    /* ================= budowa ================= */

    @Override
    protected void init() {
        px = (width - PW) / 2;
        ph = Math.min(PH_MAX, height - 8);
        py = (height - ph) / 2;

        // zakładki
        int tw = (PW - 28) / Tab.values().length;
        int i = 0;
        for (Tab t : Tab.values()) {
            FlatButton b = new FlatButton(px + 14 + i * tw, py + 33, tw - 2, 18, Text.literal(t.label), ACCENT, () -> {
                tab = t; lastTab = t; scroll = 0; msg = ""; rebuild();
            });
            b.selected = (t == tab);
            addDrawableChild(b);
            i++;
        }

        addDrawableChild(new FlatButton(px + 14, py + ph - 24, PW - 28, 18,
                Text.literal("Zamknij"), RED, this::close));

        switch (tab) {
            case PLAYERS -> initPlayers();
            case ONLINE -> initOnline();
            case TRACERS -> initTracers();
            case SETTINGS -> initSettings();
        }
    }

    private void rebuild() { clearAndInit(); }

    private TextFieldWidget makeField(int x, int y, int w, String placeholder) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, w, 18, Text.literal("Nick"));
        f.setMaxLength(16);
        f.setPlaceholder(Text.literal(placeholder).formatted(Formatting.DARK_GRAY));
        f.setText(keep);
        f.setChangedListener(s -> {
            keep = s;
            if (tab == Tab.ONLINE) { scroll = 0; dirty = true; }
        });
        addDrawableChild(f);
        setInitialFocus(f);
        return f;
    }

    /* ---- zakładka: Gracze ---- */

    private void initPlayers() {
        field = makeField(px + 14, py + 58, PW - 28 - 74, "Wpisz nick gracza...");
        addDrawableChild(new FlatButton(px + PW - 14 - 70, py + 58, 70, 18,
                Text.literal("+ Dodaj"), GREEN, this::addTyped));

        List<String> all = new ArrayList<>(TrackerState.C.tracked.keySet());
        listY = py + 114;
        visRows = Math.max(1, (ph - 42 - 114) / ROW);
        totalRows = all.size();
        scroll = Math.max(0, Math.min(scroll, totalRows - visRows));
        rowNames = new ArrayList<>(all.subList(scroll, Math.min(all.size(), scroll + visRows)));
        rowEntries.clear();

        int right = px + PW - 22;
        for (int i = 0; i < rowNames.size(); i++) {
            final String n = rowNames.get(i);
            final TrackerState.Entry e = TrackerState.C.tracked.get(n);
            int y = listY + i * ROW;

            FlatButton sw = new FlatButton(px + 15, y + 3, 10, ROW - 6, Text.empty(), 0, () -> {
                TrackerState.cycleColor(n); TrackerState.save(); rebuild();
            });
            sw.solid = TrackerState.COLORS[Math.floorMod(e.color, TrackerState.COLORS.length)];
            addDrawableChild(sw);

            addDrawableChild(new FlatButton(right - 58, y + 1, 34, ROW - 3,
                    Text.literal(e.visible ? "§aON" : "§7OFF"), e.visible ? GREEN : GRAY, () -> {
                e.visible = !e.visible; TrackerState.save(); rebuild();
            }));
            addDrawableChild(new FlatButton(right - 22, y + 1, 22, ROW - 3,
                    Text.literal("X"), RED, () -> {
                TrackerState.remove(n);
                say("Usunięto " + n + ".", 0xFF5555);
                rebuild();
            }));
        }
    }

    /* ---- zakładka: Online ---- */

    private List<PlayerListEntry> onlineList() {
        List<PlayerListEntry> out = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null || mc.player == null) return out;
        String me = mc.player.getGameProfile().getName();
        String f = keep.trim().toLowerCase();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().getName();
            if (n.equalsIgnoreCase(me)) continue;
            if (!f.isEmpty() && !n.toLowerCase().contains(f)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparing(e -> e.getProfile().getName().toLowerCase()));
        return out;
    }

    private void initOnline() {
        field = makeField(px + 14, py + 58, PW - 28, "Szukaj wśród graczy online...");
        List<PlayerListEntry> all = onlineList();
        listY = py + 88;
        visRows = Math.max(1, (ph - 42 - 88) / ROW);
        totalRows = all.size();
        scroll = Math.max(0, Math.min(scroll, totalRows - visRows));
        rowEntries = new ArrayList<>(all.subList(scroll, Math.min(all.size(), scroll + visRows)));
        rowNames.clear();

        int right = px + PW - 22;
        for (int i = 0; i < rowEntries.size(); i++) {
            final String n = rowEntries.get(i).getProfile().getName();
            int y = listY + i * ROW;
            boolean tracked = TrackerState.C.tracked.containsKey(n);
            addDrawableChild(new FlatButton(right - 66, y + 1, 66, ROW - 3,
                    Text.literal(tracked ? "§cUsuń" : "§a+ Dodaj"), tracked ? RED : GREEN, () -> {
                if (TrackerState.C.tracked.containsKey(n)) {
                    TrackerState.remove(n);
                    say("Usunięto " + n + ".", 0xFF5555);
                } else {
                    TrackerState.Result r = TrackerState.tryAdd(n);
                    say(r.msg(), r.color());
                }
                rebuild();
            }));
        }
    }

    /* ---- zakładka: Tracery ---- */

    private static String onoff(boolean b) { return b ? "§aON" : "§cOFF"; }

    private void grid(int row, int col, int span, String text, int accent, Runnable r) {
        int half = (PW - 28 - 6) / 2;
        int x = px + 14 + (col == 1 ? half + 6 : 0);
        int w = span == 2 ? PW - 28 : half;
        addDrawableChild(new FlatButton(x, py + 60 + row * 26, w, 22, Text.literal(text), accent, r));
    }

    private void initTracers() {
        TrackerState.Config c = TrackerState.C;
        grid(0, 0, 1, "Tracery: " + onoff(c.enabled), c.enabled ? GREEN : RED, () -> { c.enabled = !c.enabled; saveRebuild(); });
        grid(0, 1, 1, "Przez ściany: " + onoff(c.throughWalls), ACCENT, () -> { c.throughWalls = !c.throughWalls; saveRebuild(); });
        grid(1, 0, 1, "Styl: §f" + TrackerState.STYLE_NAMES[c.style], ACCENT, () -> { c.style = (c.style + 1) % 3; saveRebuild(); });
        grid(1, 1, 1, "Start: §f" + TrackerState.ORIGIN_NAMES[c.origin], ACCENT, () -> { c.origin = (c.origin + 1) % 2; saveRebuild(); });
        grid(2, 0, 1, "Grubość: §f" + c.thickness, ACCENT, () -> { c.thickness = c.thickness >= 5 ? 1 : c.thickness + 1; saveRebuild(); });
        grid(2, 1, 1, "Przezroczystość: §f" + TrackerState.OPACITY[c.opacityIdx] + "%", ACCENT,
                () -> { c.opacityIdx = (c.opacityIdx + 1) % TrackerState.OPACITY.length; saveRebuild(); });
        int md = TrackerState.MAX_DIST[c.maxDistIdx];
        grid(3, 0, 1, "Maks. dystans: §f" + (md == 0 ? "bez limitu" : md + "m"), ACCENT,
                () -> { c.maxDistIdx = (c.maxDistIdx + 1) % TrackerState.MAX_DIST.length; saveRebuild(); });
        grid(3, 1, 1, "Tęczowe kolory: " + onoff(c.rainbow), GOLD, () -> { c.rainbow = !c.rainbow; saveRebuild(); });
    }

    /* ---- zakładka: Ustawienia ---- */

    private void initSettings() {
        TrackerState.Config c = TrackerState.C;
        grid(0, 0, 1, "HUD: " + onoff(c.hud), ACCENT, () -> { c.hud = !c.hud; saveRebuild(); });
        grid(0, 1, 1, "Róg HUD: §f" + TrackerState.CORNER_NAMES[c.hudCorner], ACCENT,
                () -> { c.hudCorner = (c.hudCorner + 1) % 4; saveRebuild(); });
        grid(1, 0, 1, "HUD – HP: " + onoff(c.hudHealth), ACCENT, () -> { c.hudHealth = !c.hudHealth; saveRebuild(); });
        grid(1, 1, 1, "HUD – dystans: " + onoff(c.hudDistance), ACCENT, () -> { c.hudDistance = !c.hudDistance; saveRebuild(); });
        grid(2, 0, 2, "Alerty (dołączył / wyszedł / w zasięgu): " + onoff(c.alerts), GOLD,
                () -> { c.alerts = !c.alerts; saveRebuild(); });
        grid(3, 0, 1, "Reset ustawień", GRAY, () -> {
            TrackerState.Config n = new TrackerState.Config();
            n.tracked = c.tracked;
            TrackerState.C = n;
            say("Przywrócono domyślne ustawienia.", 0xFFAA00);
            saveRebuild();
        });
        grid(3, 1, 1, "Wyczyść listę", RED, () -> {
            c.tracked.clear();
            say("Wyczyszczono listę obserwowanych.", 0xFF5555);
            saveRebuild();
        });
    }

    private void saveRebuild() { TrackerState.save(); rebuild(); }

    /* ================= akcje ================= */

    private void say(String m, int color) { msg = m; msgColor = color; msgTime = Util.getMeasuringTimeMs(); }

    private void addTyped() {
        TrackerState.Result r = TrackerState.tryAdd(keep);
        say(r.msg(), r.color());
        if (r.ok()) keep = "";
        rebuild();
    }

    @Override
    public void tick() {
        if (dirty) { dirty = false; rebuild(); }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean typing = field != null && field.isFocused();
        if (tab == Tab.PLAYERS && typing && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            addTyped();
            return true;
        }
        if (!typing && PlayerTrackerClient.openKey.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        if (tab == Tab.PLAYERS || tab == Tab.ONLINE) {
            int next = scroll + (vAmount < 0 ? 1 : -1);
            int max = Math.max(0, totalRows - visRows);
            next = Math.max(0, Math.min(next, max));
            if (next != scroll) { scroll = next; dirty = true; }
            return true;
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    @Override
    public boolean shouldPause() { return false; }

    /* ================= rysowanie ================= */

    private String status(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractClientPlayerEntity p = TracerRenderer.find(mc, name);
        if (p != null && mc.player != null)
            return "§a● " + (int) p.distanceTo(mc.player) + "m  §c" + (int) Math.ceil(p.getHealth()) + "HP";
        if (TrackerState.findEntry(name) != null) return "§e● poza zasięgiem";
        return "§c● offline";
    }

    private void scrollbar(DrawContext ctx) {
        if (totalRows <= visRows) return;
        int x = px + PW - 12;
        int trackH = visRows * ROW;
        ctx.fill(x, listY, x + 3, listY + trackH, 0x30FFFFFF);
        int thumbH = Math.max(12, trackH * visRows / totalRows);
        int thumbY = listY + (trackH - thumbH) * scroll / Math.max(1, totalRows - visRows);
        ctx.fill(x, thumbY, x + 3, thumbY + thumbH, 0xFF000000 | ACCENT);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        super.renderBackground(ctx, mx, my, delta);
        MinecraftClient mc = MinecraftClient.getInstance();

        // panel + cień
        ctx.fill(px - 2, py - 2, px + PW + 2, py + ph + 2, 0x50000000);
        ctx.fillGradient(px, py, px + PW, py + ph, 0xF0101B26, 0xF00A1118);
        ctx.drawBorder(px, py, PW, ph, 0xFF000000 | ACCENT);

        // nagłówek
        ctx.fillGradient(px + 1, py + 1, px + PW - 1, py + 29, 0xFF0F3F58, 0xFF0B2233);
        ctx.fill(px + 1, py + 29, px + PW - 1, py + 30, 0xFF000000 | ACCENT);
        String me = mc.player != null ? mc.player.getGameProfile().getName() : "?";
        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry self = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (self != null) {
                ctx.fill(px + 7, py + 5, px + 25, py + 23, 0xFF000000 | ACCENT);
                PlayerSkinDrawer.draw(ctx, self.getSkinTextures(), px + 8, py + 6, 16);
            }
        }
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("PLAYER TRACKER").formatted(Formatting.AQUA, Formatting.BOLD), px + 32, py + 6, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(me + "  §a● Online"), px + 32, py + 17, 0xAAB7C2);

        String pill = TrackerState.C.enabled ? "§aTRACERY ON" : "§cTRACERY OFF";
        String cnt = "§7Obserwowani: §f" + TrackerState.C.tracked.size() + "§8/§f" + TrackerState.MAX;
        ctx.drawTextWithShadow(textRenderer, Text.literal(pill), px + PW - 10 - textRenderer.getWidth(Text.literal(pill)), py + 6, 0xFFFFFF);
        ctx.drawTextWithShadow(textRenderer, Text.literal(cnt), px + PW - 10 - textRenderer.getWidth(Text.literal(cnt)), py + 17, 0xFFFFFF);

        // pasek zakładek
        ctx.fill(px + 1, py + 31, px + PW - 1, py + 53, 0x50000000);

        switch (tab) {
            case PLAYERS -> drawPlayers(ctx, mx, my);
            case ONLINE -> drawOnline(ctx, mx, my);
            case TRACERS -> hint(ctx, py + 60 + 4 * 26 + 4,
                    "Klawisz H – śledź / przestań śledzić gracza na celowniku.",
                    "Kolor linii zmienisz klikając próbnik w zakładce Gracze.");
            case SETTINGS -> hint(ctx, py + 60 + 4 * 26 + 4,
                    "Alerty działają dla graczy z Twojej listy obserwowanych.",
                    "Lista i ustawienia zapisują się automatycznie.");
        }

        if (!msg.isEmpty() && Util.getMeasuringTimeMs() - msgTime < 6000)
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(msg), px + PW / 2, py + ph - 38, msgColor | 0xFF000000);
    }

    private void hint(DrawContext ctx, int y, String a, String b) {
        if (y + 22 > py + ph - 30) return;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(a), px + PW / 2, y, 0x7F8C99);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(b), px + PW / 2, y + 11, 0x7F8C99);
    }

    private void drawPlayers(DrawContext ctx, int mx, int my) {
        // podgląd wpisywanego gracza
        String typed = keep.trim();
        int py2 = py + 80;
        if (!typed.isEmpty()) {
            PlayerListEntry e = TrackerState.findEntry(typed);
            if (e != null) {
                PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), px + 14, py2, 16);
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal("§f" + e.getProfile().getName() + "  §a● Online  §8(Enter – dodaj)"), px + 36, py2 + 4, 0xFFFFFF);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal("§7" + typed + "  §c● offline / nie istnieje"),
                        px + 14, py2 + 4, 0xFFFFFF);
            }
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Wyszukaj gracza po nicku"), px + 14, py2 + 4, 0x6F7C88);
        }

        ctx.drawTextWithShadow(textRenderer, Text.literal("OBSERWOWANI").formatted(Formatting.AQUA),
                px + 14, py + 102, 0xFFFFFF);
        ctx.fill(px + 14, py + 112, px + PW - 14, py + 113, 0x40FFFFFF);

        int right = px + PW - 22;
        for (int i = 0; i < rowNames.size(); i++) {
            String n = rowNames.get(i);
            int y = listY + i * ROW;
            boolean hov = mx >= px + 14 && mx <= right && my >= y && my < y + ROW - 1;
            ctx.fill(px + 14, y, right, y + ROW - 1, hov ? 0x34FFFFFF : 0x1CFFFFFF);

            PlayerListEntry e = TrackerState.findEntry(n);
            if (e != null) PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), px + 29, y + 2, 14);
            else ctx.fill(px + 29, y + 2, px + 43, y + 16, 0xFF2A3540);

            int col = 0xFF000000 | TrackerState.rgb(n);
            ctx.drawTextWithShadow(textRenderer, Text.literal(n), px + 48, y + 5, col);
            ctx.drawTextWithShadow(textRenderer, Text.literal(status(n)), px + 140, y + 5, 0xFFFFFF);
        }
        if (rowNames.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Brak obserwowanych graczy."),
                    px + PW / 2, listY + 10, 0x6F7C88);
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Wpisz nick powyżej albo wybierz z zakładki Online."),
                    px + PW / 2, listY + 22, 0x56626D);
        }
        scrollbar(ctx);
    }

    private void drawOnline(DrawContext ctx, int mx, int my) {
        ctx.drawTextWithShadow(textRenderer,
                Text.literal("GRACZE ONLINE §7(" + totalRows + ")").formatted(Formatting.AQUA), px + 14, py + 79, 0xFFFFFF);
        ctx.fill(px + 14, py + 86, px + PW - 14, py + 87, 0x40FFFFFF);

        MinecraftClient mc = MinecraftClient.getInstance();
        int right = px + PW - 22;
        for (int i = 0; i < rowEntries.size(); i++) {
            PlayerListEntry e = rowEntries.get(i);
            String n = e.getProfile().getName();
            int y = listY + i * ROW;
            boolean hov = mx >= px + 14 && mx <= right && my >= y && my < y + ROW - 1;
            ctx.fill(px + 14, y, right, y + ROW - 1, hov ? 0x34FFFFFF : 0x1CFFFFFF);
            PlayerSkinDrawer.draw(ctx, e.getSkinTextures(), px + 17, y + 2, 14);

            boolean tracked = TrackerState.C.tracked.containsKey(n);
            ctx.drawTextWithShadow(textRenderer, Text.literal(n), px + 36, y + 5,
                    tracked ? 0xFF000000 | TrackerState.rgb(n) : 0xFFFFFFFF);

            AbstractClientPlayerEntity p = TracerRenderer.find(mc, n);
            String info = (p != null && mc.player != null ? "§7" + (int) p.distanceTo(mc.player) + "m  " : "§8poza zasięgiem  ")
                    + "§8" + e.getLatency() + "ms";
            ctx.drawTextWithShadow(textRenderer, Text.literal(info), px + 140, y + 5, 0xFFFFFF);
        }
        if (rowEntries.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Brak graczy do wyświetlenia."),
                    px + PW / 2, listY + 10, 0x6F7C88);
        scrollbar(ctx);
    }
}
