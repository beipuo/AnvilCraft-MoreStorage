package dev.anvilcraft.addon.morestorage.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Widens "is the hyperdimension terminal" to "is a hyperdimension terminal".
 *
 * <p>The four checks touched here all compare against the exact terminal item, which means a
 * subclass of it would be treated as an ordinary stack: the storage RPC would refuse to serve it, a
 * storage would happily swallow it, item balancing would ignore the storage it is bound to, and
 * clicking a stack onto it in the inventory would do nothing. Answering the item-identity question
 * by type instead lets any terminal subclass — this addon's crafting terminal, or a later one —
 * behave like the terminal it is.
 *
 * <p>Each of these methods compares a stack against several terminals, not just this one, so the
 * handler is told which terminal the call site asked about and widens only the hyperdimension one.
 * That keeps it indifferent to how many such comparisons a method makes and in what order, which is
 * what the previous ordinal-free {@code ModifyExpressionValue} was not.
 */
@Mixin(StorageServerStub.class)
public abstract class StorageServerStubMixin {
    @WrapOperation(
        method = {
            "isBoundTerminal(Lnet/minecraft/world/item/ItemStack;Ljava/util/UUID;Ljava/util/UUID;)Z",
            "canStore(Ldev/dubhe/anvilcraft/saved/storage/BaseStorage;Lnet/minecraft/world/item/ItemStack;)Z",
            "collectBoundStorage(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)V",
            "terminalTargetId(Lnet/minecraft/server/level/ServerPlayer;"
            + "Lnet/minecraft/world/item/ItemStack;)Ljava/util/UUID;"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private static boolean moreStorage$isAnyTerminal(
        ItemStack stack,
        Holder<Item> terminal,
        Operation<Boolean> original
    ) {
        if (original.call(stack, terminal)) {
            return true;
        }
        // The local and shulker terminals are siblings, not superclasses, so this holds only for the
        // hyperdimension terminal's own comparison.
        return terminal.value() instanceof HyperdimensionTerminalItem
               && stack.getItem() instanceof HyperdimensionTerminalItem;
    }
}
