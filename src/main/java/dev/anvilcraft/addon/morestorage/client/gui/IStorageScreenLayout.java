package dev.anvilcraft.addon.morestorage.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The parts of AnvilCraft's storage screen this addon needs to move around.
 *
 * <p>{@code StorageScreen} keeps its layout in private constants, so a subclass cannot change the
 * background it blits or where the player inventory sits. The mixin on that class reads these
 * methods instead of the constants whenever the screen happens to implement this interface, which
 * leaves the original screen untouched and gives a taller variant somewhere to state its numbers.
 */
public interface IStorageScreenLayout {
    /** The background atlas to blit instead of {@code storage_station}. */
    ResourceLocation moreStorage$background();

    /** How tall the background is, in screen pixels. */
    int moreStorage$backgroundHeight();

    /** How tall the background <em>atlas</em> is, for the blit's UV maths. */
    int moreStorage$backgroundTextureHeight();

    /** Y of the top row of the player inventory, relative to the top of the background. */
    int moreStorage$inventoryY();

    /** Y of the deposit button, relative to the top of the background. */
    int moreStorage$depositButtonY();

    /** Y of the withdraw button, relative to the top of the background. */
    int moreStorage$withdrawButtonY();

    /** How many rows the category list on the left shows at once. */
    int moreStorage$categoryRows();

    /**
     * Draws whatever the taller background added, after the inventory and before the widgets and
     * the cursor — so items here sit under the stack the mouse is carrying, as they should.
     */
    void moreStorage$renderContents(GuiGraphics graphics, int mouseX, int mouseY);

    /** Draws on top of everything, once the rest of the screen is done. Tooltips live here. */
    void moreStorage$renderOverlay(GuiGraphics graphics, int mouseX, int mouseY);
}
