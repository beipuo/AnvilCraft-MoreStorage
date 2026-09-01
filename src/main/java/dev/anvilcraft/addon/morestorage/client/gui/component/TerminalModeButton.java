package dev.anvilcraft.addon.morestorage.client.gui.component;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.client.gui.screen.TerminalLayout;
import dev.anvilcraft.addon.morestorage.terminal.TerminalMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * One of the four buttons down the right-hand side that choose what the terminal is.
 *
 * <p>{@code select_button.png} holds one icon per mode, stacked in {@link TerminalMode} order, and
 * nothing else — no pressed row, no hovered row — so both of those states are drawn over the icon
 * instead of blitted: the mode in use is left at full brightness and the other three are dimmed, which
 * is what makes the strip readable as a set of tabs with one of them open.
 */
public class TerminalModeButton extends Button {
    private static final ResourceLocation TEXTURE =
        AnvilCraftMoreStorage.of("textures/gui/misc/background/select_button.png");

    /** The strip sits at this offset inside a 512×512 atlas, one 18×18 icon per mode. */
    private static final int TEXTURE_SIZE = 512;
    private static final int U = 277;
    private static final int V = 139;

    /** Black over the three modes that are not in use. */
    private static final int DIMMED = 0x60000000;
    /** White over whichever one the mouse is on. */
    private static final int HIGHLIGHT = 0x30FFFFFF;

    private final TerminalMode mode;
    private final Supplier<TerminalMode> current;

    public TerminalModeButton(int x, int y, TerminalMode mode, Supplier<TerminalMode> current, OnPress onPress) {
        super(
            x,
            y,
            TerminalLayout.MODE_BUTTON_SIZE,
            TerminalLayout.MODE_BUTTON_SIZE,
            Component.translatable(mode.labelKey()),
            onPress,
            Button.DEFAULT_NARRATION
        );
        this.mode = mode;
        this.current = current;
        this.setTooltip(Tooltip.create(Component.translatable(mode.labelKey())));
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(
            TerminalModeButton.TEXTURE,
            this.getX(),
            this.getY(),
            TerminalModeButton.U,
            TerminalModeButton.V + TerminalLayout.MODE_BUTTON_STRIDE * this.mode.ordinal(),
            this.width,
            this.height,
            TerminalModeButton.TEXTURE_SIZE,
            TerminalModeButton.TEXTURE_SIZE
        );
        if (this.current.get() != this.mode) {
            graphics.fill(
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                TerminalModeButton.DIMMED
            );
        }
        if (this.isHovered()) {
            graphics.fill(
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                TerminalModeButton.HIGHLIGHT
            );
        }
    }
}
