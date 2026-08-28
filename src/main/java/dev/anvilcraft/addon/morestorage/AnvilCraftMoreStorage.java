package dev.anvilcraft.addon.morestorage;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.addon.morestorage.data.AddonDatagen;
import dev.anvilcraft.addon.morestorage.init.AddonBlockEntities;
import dev.anvilcraft.addon.morestorage.init.AddonBlocks;
import dev.anvilcraft.addon.morestorage.init.AddonComponents;
import dev.anvilcraft.addon.morestorage.init.AddonItemGroups;
import dev.anvilcraft.addon.morestorage.init.AddonItems;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Storage extensions for AnvilCraft.
 *
 * <p>The crates are a copy of AnvilCraft's crate and large crate per tier, whose backing storage is
 * widened by a configurable multiplier. They subclass AnvilCraft's own blocks, which is what keeps
 * them interchangeable with plain crates: they share the storage screen, the item handler capability,
 * and the 3x3x3 neighbour pooling that lets adjacent crates be searched as one inventory.
 *
 * <p>The crafting terminals are the same idea applied to AnvilCraft's terminals — one subclass per
 * terminal item, each adding a crafting grid that pulls its ingredients from whatever storage that
 * terminal connects to.
 */
@Mod(AnvilCraftMoreStorage.MOD_ID)
public class AnvilCraftMoreStorage {
    public static final String MOD_ID = "anvilcraft_more_storage";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final AddonConfig CONFIG = ConfigManager.register(AnvilCraftMoreStorage.MOD_ID, AddonConfig::new);
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    public AnvilCraftMoreStorage(IEventBus modEventBus, ModContainer modContainer) {
        AddonItemGroups.register(modEventBus);
        AddonComponents.register(modEventBus);
        AddonBlocks.register();
        AddonItems.register();
        AddonBlockEntities.register();
        AddonDatagen.init();
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
