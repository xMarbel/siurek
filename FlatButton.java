package pl.playertracker;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Płaski, animowany przycisk w stylu "premium". */
public class FlatButton extends ClickableWidget {

    private final Runnable action;
    private final int accent;
    private float hover = 0f;
    public boolean selected = false;
    public int solid = 0; // != 0 => rysuje jednolity kolor (próbnik koloru)

    public FlatButton(int x, int y, int w, int h, Text label, int accent, Runnable action) {
        super(x, y, w, h, label);
        this.accent = accent;
        this.action = action;
    }

    private static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t), g = (int) (ag + (bg - ag) * t), bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
        boolean hov = isHovered();
        hover += ((hov ? 1f : 0f) - hover) * 0.3f;
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        if (solid != 0) {
            ctx.fill(x, y, x + w, y + h, 0xFF000000 | solid);
            ctx.drawBorder(x, y, w, h, hov ? 0xFFFFFFFF : 0xFF000000);
            return;
        }

        int base = selected ? 0xFF1C3A4D : 0xFF16212B;
        int hoverCol = lerp(base, 0xFF000000 | accent, 0.30f);
        ctx.fill(x, y, x + w, y + h, lerp(base, hoverCol, hover));
        ctx.fill(x, y, x + w, y + 1, 0x18FFFFFF);                 // delikatny blask u góry
        if (selected) ctx.fill(x, y + h - 2, x + w, y + h, 0xFF000000 | accent);
        else ctx.fill(x, y, x + 2, y + h, 0xFF000000 | accent);   // pasek akcentu

        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, getMessage(), x + w / 2 + 1, y + (h - 8) / 2, 0xFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) { action.run(); }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) { appendDefaultNarrations(builder); }
}
