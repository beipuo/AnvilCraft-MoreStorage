package dev.anvilcraft.addon.morestorage.terminal;

import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import dev.anvilcraft.addon.morestorage.mixin.StorageServerStubInvoker;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.item.ShulkerTerminalItem;
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
 * A shulker terminal with a crafting grid bolted on.
 *
 * <p>Same relationship to {@link ShulkerTerminalItem} as the hyperdimension crafting terminal has to
 * its own base: the target is inherited whole — the first shulker container the player carries, else
 * every shulker box in their inventory at once, else the nearest world container within 64 blocks —
 * and only the screen it opens and the nine crafting slots it carries are this addon's. The target is
 * resolved again on every use, so what the grid restocks from follows what the player is carrying.
 */
public class ShulkerCraftingTerminalItem extends ShulkerTerminalItem implements CraftingTerminal {
    /** The extra tooltip line, following upstream's {@code item.<namespace>.<path>.<suffix>} naming. */
    public static final String CRAFTING_KEY = "item.anvilcraft_more_storage.shulker_crafting_terminal.crafting";

    /** {@link #CRAFTING_KEY} in {@code en_us}. */
    public static final String CRAFTING_LANG = "Crafts from the connected shulker boxes";

    public ShulkerCraftingTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            ShulkerCraftingTerminalItem.openConnectedCrafter(player);
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
        tooltipComponents.add(Component.translatable(ShulkerCraftingTerminalItem.CRAFTING_KEY));
    }

    /**
     * The shulker terminal's target is derived from the player, not stored on the stack, so every
     * shulker terminal a player carries names the same target — which is also what upstream's own
     * terminal does.
     */
    @Override
    public @Nullable UUID craftingTarget(Player player, ItemStack stack) {
        return StorageServerStubInvoker.moreStorage$shulkerTerminalId(player.getGameProfile().getId());
    }

    /**
     * Opens the crafting terminal on the shulker target this terminal connects to.
     *
     * <p>Same handshake as the plain shulker terminal: ask the server for a virtual position standing in
     * for the resolved target, which also fails when there is none. Only the screen differs.
     */
    public static void openConnectedCrafter(Player player) {
        if (!player.level().isClientSide()) {
            return;
        }
        UUID targetId = StorageTerminalClientStub.shulkerTerminalId();
        StorageTerminalClientStub.openRemote(targetId)
            .exceptionally(ignored -> -1L)
            .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                if (virtualPos == -1L) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.shulker_terminal.not_found"),
                        true
                    );
                    return;
                }
                CraftingTerminalScreen.openScreen(
                    BlockPos.of(virtualPos),
                    targetId,
                    Component.translatable("block.anvilcraft.shulker_container")
                );
            }));
    }
}
