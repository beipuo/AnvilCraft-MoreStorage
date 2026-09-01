package dev.anvilcraft.addon.morestorage.integration.emi;

import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import dev.anvilcraft.addon.morestorage.client.gui.screen.TerminalLayout;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;

/** Keeps EMI's sidebars clear of the terminal's four mode buttons. */
@EmiEntrypoint
public final class MoreStorageEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(CraftingTerminalScreen.class, (screen, add) -> add.accept(new Bounds(
            screen.getGuiLeft() + TerminalLayout.MODE_BUTTON_X,
            screen.getGuiTop() + TerminalLayout.MODE_BUTTON_Y,
            TerminalLayout.MODE_BUTTON_SIZE,
            TerminalLayout.MODE_BUTTON_SIZE
                + TerminalLayout.MODE_BUTTON_STRIDE * (4 - 1)
        )));
    }
}
