package dev.anvilcraft.addon.morestorage.terminal;

import dev.anvilcraft.addon.morestorage.client.gui.screen.CraftingTerminalScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
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
 * A hyperdimension terminal with a crafting grid bolted on.
 *
 * <p>Binding, remote access and the storage list are all inherited from
 * {@link HyperdimensionTerminalItem} — the only differences are the screen it opens and the nine
 * crafting slots it carries in {@link CraftingTerminalGrid}. AnvilCraft's own terminal checks are
 * widened from "is exactly the terminal item" to "is a {@link HyperdimensionTerminalItem}" by this
 * addon's mixins, which is what lets a subclass bind to a station and talk to the storage RPC at all.
 */
public class HyperdimensionCraftingTerminalItem extends HyperdimensionTerminalItem {
    /**
     * The extra tooltip line, following upstream's {@code item.<namespace>.<path>.<suffix>} naming for
     * the terminal's own bound / unbound lines.
     */
    public static final String CRAFTING_KEY = "item.anvilcraft_more_storage.hyperdimension_crafting_terminal.crafting";

    /** {@link #CRAFTING_KEY} in {@code en_us}. */
    public static final String CRAFTING_LANG = "Crafts from the bound storage";

    public HyperdimensionCraftingTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            HyperdimensionCraftingTerminalItem.openBoundCrafter(player, stack);
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
        tooltipComponents.add(Component.translatable(HyperdimensionCraftingTerminalItem.CRAFTING_KEY));
    }

    /** The storage this terminal is bound to, or {@code null} while it is unbound. */
    public static @Nullable UUID boundStorage(ItemStack stack) {
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        return binding == null ? null : binding.id().orElse(null);
    }

    /**
     * Opens the crafting terminal on the bound storage.
     *
     * <p>Same handshake as the plain terminal: ask the server for a virtual position standing in for
     * the storage, then open the screen on it. Only the screen differs.
     */
    public static void openBoundCrafter(Player player, ItemStack stack) {
        if (!player.level().isClientSide()) {
            return;
        }
        UUID storageId = HyperdimensionCraftingTerminalItem.boundStorage(stack);
        if (storageId == null) {
            player.displayClientMessage(
                Component.translatable("message.anvilcraft.hyperdimension_terminal.not_bound"),
                true
            );
            return;
        }
        StorageTerminalClientStub.openRemote(storageId)
            .exceptionally(ignored -> -1L)
            .thenAccept(virtualPos -> Minecraft.getInstance().execute(() -> {
                if (virtualPos == -1L) {
                    player.displayClientMessage(
                        Component.translatable("message.anvilcraft.hyperdimension_terminal.not_found"),
                        true
                    );
                    return;
                }
                CraftingTerminalScreen.openScreen(
                    BlockPos.of(virtualPos),
                    storageId,
                    Component.translatable("block.anvilcraft.hyperdimension_storage_station")
                );
            }));
    }
}
