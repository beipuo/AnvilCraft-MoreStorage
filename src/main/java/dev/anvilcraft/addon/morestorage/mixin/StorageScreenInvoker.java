package dev.anvilcraft.addon.morestorage.mixin;

import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Reaches {@code StorageScreen}'s private storage-list refresh.
 *
 * <p>A subclass that moves items in or out of the storage on its own has to ask for the same
 * refresh the deposit and withdraw buttons do, or the list on the right keeps showing what was
 * there before the craft.
 */
@Mixin(StorageScreen.class)
public interface StorageScreenInvoker {
    @Invoker("reorder")
    void moreStorage$reorder(boolean resetScroll);
}
