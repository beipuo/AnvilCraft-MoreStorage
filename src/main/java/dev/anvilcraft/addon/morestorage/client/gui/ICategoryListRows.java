package dev.anvilcraft.addon.morestorage.client.gui;

/**
 * A category list whose visible row count can be changed after construction.
 *
 * <p>{@code CategoryList} folds {@code COLUMN * ROW} into the literal {@code 8} in every method
 * that pages or scrolls; the mixin replaces that literal with this value so the taller background
 * can show more categories without a second widget class.
 */
public interface ICategoryListRows {
    int moreStorage$rows();

    void moreStorage$setRows(int rows);
}
