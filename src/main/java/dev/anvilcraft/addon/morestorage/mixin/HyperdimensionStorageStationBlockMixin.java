package dev.anvilcraft.addon.morestorage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.addon.morestorage.terminal.HyperdimensionCraftingTerminalItem;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a terminal subclass be bound to, and opened from, a hyperdimension storage station.
 *
 * <p>Right-clicking the station is the only way a terminal ever gets bound, and the station only
 * recognises the exact terminal item — so without the first handler this addon's crafting terminal
 * could never be bound at all. The second sends the follow-up right-click, the one that opens an
 * already-bound terminal, to whichever screen that terminal owns.
 */
@Mixin(HyperdimensionStorageStationBlock.class)
public abstract class HyperdimensionStorageStationBlockMixin {
    @ModifyExpressionValue(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private boolean moreStorage$isAnyTerminal(boolean original, @Local ItemStack stack) {
        return original || stack.getItem() instanceof HyperdimensionTerminalItem;
    }

    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/item/HyperdimensionTerminalItem;"
                     + "openBoundStorage(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void moreStorage$openBound(Player player, ItemStack stack) {
        if (stack.getItem() instanceof HyperdimensionCraftingTerminalItem) {
            HyperdimensionCraftingTerminalItem.openBoundCrafter(player, stack);
        } else {
            HyperdimensionTerminalItem.openBoundStorage(player, stack);
        }
    }
}
