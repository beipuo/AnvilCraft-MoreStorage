package dev.anvilcraft.addon.morestorage.init;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.crate.CrateTier;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage.REGISTRUM;

/**
 * A single tab holding every tier's crate and large crate.
 *
 * <p>The contents are filled in by Registrum, from the default creative tab set in
 * {@link AddonBlocks}, so they follow registration order: copper through netherite, each tier's
 * crate followed by its large crate.
 */
public class AddonItemGroups {
    private static final DeferredRegister<CreativeModeTab> DEFERRED_REGISTER = DeferredRegister.create(
        Registries.CREATIVE_MODE_TAB,
        AnvilCraftMoreStorage.MOD_ID
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MORE_STORAGE = DEFERRED_REGISTER.register(
        "main",
        () -> CreativeModeTab.builder()
            .icon(AddonBlocks.crate(CrateTier.NETHERITE)::asStack)
            .displayItems((ctx, entries) -> {
            })
            .title(
                REGISTRUM.addLang(
                    "itemGroup",
                    AnvilCraftMoreStorage.of("main"),
                    "AnvilCraft More Storage"
                )
            )
            // AnvilCraft asks addons to hang their tab after its own items tab.
            .withTabsBefore(ModItemGroups.ANVILCRAFT_ITEMS.getId())
            .build()
    );

    private AddonItemGroups() {
    }

    public static void register(IEventBus modEventBus) {
        DEFERRED_REGISTER.register(modEventBus);
    }
}
