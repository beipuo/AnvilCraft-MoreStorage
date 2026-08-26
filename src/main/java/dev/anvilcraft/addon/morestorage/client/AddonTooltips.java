package dev.anvilcraft.addon.morestorage.client;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.anvilcraft.addon.morestorage.crate.CrateTooltip;
import dev.anvilcraft.addon.morestorage.init.AddonBlocks;
import dev.anvilcraft.addon.morestorage.storage.TierCapacity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Gives the tiered crates the tooltip AnvilCraft's own crates have, plus a line for the tiers whose
 * material does something (see {@code CrateTrait}).
 *
 * <p>AnvilCraft adds those lines from its own {@code ItemTooltipEvent} listener, but only for items
 * present in {@code ItemTooltipManager}'s private maps, so an addon's items are skipped and have to
 * be handled here. The layout is copied from upstream so the two look alike: the lines go in at
 * index 1, immediately under the item name, in grey, and without Shift held a "hold [Shift]" hint
 * follows them.
 */
@EventBusSubscriber(modid = AnvilCraftMoreStorage.MOD_ID, value = Dist.CLIENT)
public final class AddonTooltips {
    private AddonTooltips() {
    }

    @SubscribeEvent
    static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        for (CrateTier tier : CrateTier.values()) {
            if (AddonBlocks.crate(tier).isIn(stack)) {
                addCrateTooltip(event.getToolTip(), tier, false);
                return;
            }
            if (AddonBlocks.largeCrate(tier).isIn(stack)) {
                addCrateTooltip(event.getToolTip(), tier, true);
                return;
            }
        }
    }

    private static void addCrateTooltip(List<Component> tooltip, CrateTier tier, boolean large) {
        if (Screen.hasShiftDown()) {
            addLines(
                tooltip,
                I18n.get(
                    large ? CrateTooltip.LARGE_CRATE_SHIFT_KEY : CrateTooltip.CRATE_SHIFT_KEY,
                    large ? TierCapacity.largeCrateCapacity(tier) : TierCapacity.crateCapacity(tier)
                )
            );
            return;
        }
        String text = I18n.get(large ? CrateTooltip.LARGE_CRATE_KEY : CrateTooltip.CRATE_KEY);
        @Nullable String traitId = tier.trait().tooltipId();
        if (traitId != null) {
            text = text + "\n" + I18n.get(CrateTooltip.traitKey(traitId));
        }
        int added = addLines(tooltip, text);
        tooltip.add(
            1 + added,
            Component.translatable(
                "tooltip.anvilcraft.press_key",
                Component.literal("[Shift]").withStyle(ChatFormatting.WHITE)
            ).withStyle(ChatFormatting.DARK_GRAY)
        );
    }

    /** Splits a translated tooltip on newlines the way upstream does, and returns the line count. */
    private static int addLines(List<Component> tooltip, String text) {
        String[] lines = text.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            tooltip.add(1, Component.literal(lines[i]).withStyle(ChatFormatting.GRAY));
        }
        return lines.length;
    }
}
