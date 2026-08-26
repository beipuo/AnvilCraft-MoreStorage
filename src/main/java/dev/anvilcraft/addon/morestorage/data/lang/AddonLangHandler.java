package dev.anvilcraft.addon.morestorage.data.lang;

import dev.anvilcraft.addon.morestorage.AddonConfig;
import dev.anvilcraft.addon.morestorage.crate.CrateTooltip;
import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class AddonLangHandler {

    /**
     * 语言文件初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        ConfigData.readConfigClass(provider, AddonConfig.class);
        provider.add(CrateTooltip.CRATE_KEY, CrateTooltip.CRATE);
        provider.add(CrateTooltip.CRATE_SHIFT_KEY, CrateTooltip.SHIFT);
        provider.add(CrateTooltip.LARGE_CRATE_KEY, CrateTooltip.LARGE_CRATE);
        provider.add(CrateTooltip.LARGE_CRATE_SHIFT_KEY, CrateTooltip.SHIFT);
    }
}
