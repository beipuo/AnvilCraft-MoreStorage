package dev.anvilcraft.addon.morestorage.client.gui;

/**
 * A category list whose visible row count can be changed after construction.
 *
 * <p>The mixin updates the row count in the parent's layout record so the taller background can show
 * more categories without a second widget class.
 */
public interface ICategoryListRows {
    int moreStorage$rows();

    void moreStorage$setRows(int rows);
}
