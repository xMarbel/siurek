package pl.playertracker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PlayerTrackerClient implements ClientModInitializer {

    public static KeyBinding openKey, toggleKey, lookKey;

    @Override
    public void onInitializeClient() {
        TrackerState.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playertracker.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.playertracker"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playertracker.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.playertracker"));
        lookKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playertracker.look", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.playertracker"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new TrackerScreen());
            }
            while (toggleKey.wasPressed()) {
                TrackerState.C.enabled = !TrackerState.C.enabled;
                TrackerState.save();
                if (client.player != null)
                    client.player.sendMessage(Text.literal(TrackerEvents.PREFIX + "§7Tracery: "
                            + (TrackerState.C.enabled ? "§aWŁĄCZONE" : "§cWYŁĄCZONE")), true);
            }
            while (lookKey.wasPressed()) {
                if (client.currentScreen == null) TrackerEvents.toggleLooked(client);
            }
            TrackerEvents.tick(client);
        });

        WorldRenderEvents.LAST.register(TracerRenderer::render);
        HudRenderCallback.EVENT.register(TracerRenderer::renderHud);
    }
}
