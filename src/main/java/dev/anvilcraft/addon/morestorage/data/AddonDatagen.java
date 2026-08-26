package dev.anvilcraft.addon.morestorage.data;

import dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage;
import dev.anvilcraft.addon.morestorage.data.lang.AddonLangHandler;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.anvilcraft.addon.morestorage.AnvilCraftMoreStorage.REGISTRUM;

@EventBusSubscriber(modid = AnvilCraftMoreStorage.MOD_ID)
public class AddonDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {}

    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
    }
}
