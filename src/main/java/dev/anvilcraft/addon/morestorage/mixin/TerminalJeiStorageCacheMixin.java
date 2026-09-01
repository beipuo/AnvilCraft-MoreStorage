package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminal;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Makes AnvilCraft's terminal JEI material cache include this addon's terminal subclasses. */
@Mixin(TerminalJeiStorageCache.class)
public abstract class TerminalJeiStorageCacheMixin {
    @Inject(method = "storageOf", at = @At("HEAD"), cancellable = true)
    private static void moreStorage$storageOf(ItemStack stack, CallbackInfoReturnable<UUID> cir) {
        Player player = Minecraft.getInstance().player;
        if (player != null && stack.getItem() instanceof CraftingTerminal terminal) {
            cir.setReturnValue(terminal.craftingTarget(player, stack));
        }
    }
}
