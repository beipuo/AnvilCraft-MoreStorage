package dev.anvilcraft.addon.morestorage.terminal;

import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import dev.anvilcraft.addon.morestorage.mixin.StorageServerStubInvoker;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.item.LocalTerminalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * A local terminal with a crafting grid bolted on.
 *
 * <p>Same relationship to {@link LocalTerminalItem} as the hyperdimension crafting terminal has to
 * its own base: the connection is inherited whole — the nearest large crate within 32 blocks, resolved
 * again on every use — and only the screen it opens and the nine crafting slots it carries are this
 * addon's. Because the crate is picked per use, the grid restocks from whichever crate the player
 * happens to be standing next to.
 */
public class LocalCraftingTerminalItem extends LocalTerminalItem implements CraftingTerminal {
    /** The extra tooltip line, following upstream's {@code item.<namespace>.<path>.<suffix>} naming. */
    public static final String CRAFTING_KEY = "item.anvilcraft_more_storage.local_crafting_terminal.crafting";

    /** {@link #CRAFTING_KEY} in {@code en_us}. */
    public static final String CRAFTING_LANG = "Crafts from the connected large crate";

    public LocalCraftingTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            LocalCraftingTerminalItem.openConnectedCrafter(player);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, stack);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        List<Component> tooltipComponents,
        TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(LocalCraftingTerminalItem.CRAFTING_KEY));
    }

    /**
     * The local terminal's target is derived from the player, not stored on the stack, so every local
     * terminal a player carries names the same crate — which is also what upstream's own terminal does.
     */
    @Override
    public @Nullable UUID craftingTarget(Player player, ItemStack stack) {
        return StorageServerStubInvoker.moreStorage$localTerminalId(player.getGameProfile().getId());
    }

    /**
     * Opens the crafting terminal on the crate this terminal connects to.
     *
     * <p>Same handshake as the plain local terminal: ask the server for a virtual position standing in
     * for the resolved crate, which also fails when there is none in range. Only the screen differs.
     */
    public static void openConnectedCrafter(Player player) {
        if (!player.level().isClientSide()) {
            return;
        }
        UUID targetId = StorageTerminalClientStub.localTerminalId();
        StorageTerminalClientStub.openRemote(targetId)
            .exceptionally(ignored -> -1L)
            .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                if (virtualPos == -1L) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.local_terminal.not_found"),
                        true
                    );
                    return;
                }
                CraftingTerminalScreen.openScreen(
                    BlockPos.of(virtualPos),
                    targetId,
                    Component.translatable("block.anvilcraft.large_crate")
                );
            }));
    }
}
