package dev.anvilcraft.addon.morestorage.crate;

/** Shared surface of the tiered crate block entities, which have no common AnvilCraft superclass. */
public interface TieredCrate {
    CrateTier getTier();

    /** Raise the backing AnvilCraft storage to this crate's tier capacity, creating it if needed. */
    void applyTierCapacity();
}
