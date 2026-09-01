package dev.anvilcraft.addon.morestorage.integration.jei;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import dev.anvilcraft.addon.morestorage.client.gui.screen.TerminalLayout;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Keeps JEI's ingredient list clear of the terminal's four mode buttons. */
@JeiPlugin
public final class MoreStorageJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
        AnvilCraftMoreStorage.of("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(CraftingTerminalScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(CraftingTerminalScreen screen) {
                return List.of(new Rect2i(
                    screen.getGuiLeft() + TerminalLayout.MODE_BUTTON_X,
                    screen.getGuiTop() + TerminalLayout.MODE_BUTTON_Y,
                    TerminalLayout.MODE_BUTTON_SIZE,
                    TerminalLayout.MODE_BUTTON_SIZE
                        + TerminalLayout.MODE_BUTTON_STRIDE * (4 - 1)
                ));
            }
        });
    }
}
