package dev.anvilcraft.addon.morestorage.client;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilCraftMoreStorage.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftMoreStorageClient {
    public AnvilCraftMoreStorageClient(IEventBus modBus, ModContainer container) {
    }
}
