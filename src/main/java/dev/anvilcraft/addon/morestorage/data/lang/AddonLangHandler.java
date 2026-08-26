package dev.anvilcraft.addon.morestorage.data.lang;

import dev.anvilcraft.addon.morestorage.AddonConfig;
import dev.anvilcraft.addon.morestorage.crate.CrateTier;
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
        for (CrateTier tier : CrateTier.values()) {
            provider.add(CrateTooltip.key(tier.crateName()), CrateTooltip.CRATE);
            provider.add(CrateTooltip.shiftKey(tier.crateName()), CrateTooltip.SHIFT);
            provider.add(CrateTooltip.key(tier.largeCrateName()), CrateTooltip.LARGE_CRATE);
            provider.add(CrateTooltip.shiftKey(tier.largeCrateName()), CrateTooltip.SHIFT);
        }
    }
}
