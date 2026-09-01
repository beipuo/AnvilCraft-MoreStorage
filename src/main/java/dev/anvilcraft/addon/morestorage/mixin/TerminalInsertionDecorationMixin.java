package dev.anvilcraft.addon.morestorage.mixin;

import dev.anvilcraft.addon.morestorage.terminal.CraftingTerminal;
import dev.dubhe.anvilcraft.client.renderer.item.decoration.TerminalInsertionDecoration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** Shows AnvilCraft's terminal insertion indicator for this addon's terminal subclasses. */
@Mixin(TerminalInsertionDecoration.class)
public abstract class TerminalInsertionDecorationMixin {
    @Inject(method = "terminalTargetId", at = @At("HEAD"), cancellable = true)
    private static void moreStorage$terminalTargetId(ItemStack stack, CallbackInfoReturnable<UUID> cir) {
        Player player = Minecraft.getInstance().player;
        if (player != null && stack.getItem() instanceof CraftingTerminal terminal) {
            cir.setReturnValue(terminal.craftingTarget(player, stack));
        }
    }
}
