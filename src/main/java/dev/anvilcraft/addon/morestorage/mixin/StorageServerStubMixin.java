package dev.anvilcraft.addon.morestorage.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Widens "is the hyperdimension terminal" to "is a hyperdimension terminal".
 *
 * <p>The three checks touched here all compare against the exact terminal item, which means a
 * subclass of it would be treated as an ordinary stack: the storage RPC would refuse to serve it,
 * a storage would happily swallow it, and item balancing would ignore the storage it is bound to.
 * Answering the item-identity question by type instead lets any terminal subclass — this addon's
 * crafting terminal, or a later one — behave like the terminal it is.
 */
@Mixin(StorageServerStub.class)
public abstract class StorageServerStubMixin {
    @ModifyExpressionValue(
        method = {
            "isBoundTerminal(Lnet/minecraft/world/item/ItemStack;Ljava/util/UUID;)Z",
            "canStore(Ldev/dubhe/anvilcraft/saved/storage/BaseStorage;Lnet/minecraft/world/item/ItemStack;)Z",
            "collectBoundStorage(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)V"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private static boolean moreStorage$isAnyTerminal(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getItem() instanceof HyperdimensionTerminalItem;
    }
}
