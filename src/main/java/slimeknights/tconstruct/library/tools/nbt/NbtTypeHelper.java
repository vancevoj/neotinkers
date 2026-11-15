package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/**
 * Helpers for typed tag access. Since 1.21.5 {@link CompoundTag} has no typed {@code contains(String, int)} or {@code getList(String, int)},
 * and {@link ListTag} no longer has an element type, so these reimplement the old semantics for tool data.
 */
public final class NbtTypeHelper {
  private NbtTypeHelper() {}

  /** Tag type matching any numeric tag, replacement for the removed {@code Tag.TAG_ANY_NUMERIC} */
  public static final int TAG_ANY_NUMERIC = 99;

  /**
   * Checks if the given tag has the given type
   * @param tag   Tag to check
   * @param type  Tag type from {@link Tag}, or {@link #TAG_ANY_NUMERIC}
   * @return  True if the tag is non-null and matches the type
   */
  public static boolean isType(@Nullable Tag tag, int type) {
    if (tag == null) {
      return false;
    }
    if (type == TAG_ANY_NUMERIC) {
      return tag instanceof NumericTag;
    }
    return tag.getId() == type;
  }

  /**
   * Checks if the compound contains the given key with the given type, replacement for the removed {@code CompoundTag#contains(String, int)}
   * @param nbt   Compound to check
   * @param key   Key to check
   * @param type  Tag type from {@link Tag}, or {@link #TAG_ANY_NUMERIC}
   * @return  True if the key is present with the given type
   */
  public static boolean contains(CompoundTag nbt, String key, int type) {
    return isType(nbt.get(key), type);
  }

  /**
   * Checks if the given tag is a list containing the given element type. Empty lists match any element type, as before.
   * Lists used to be homogeneous, so like the old element type this only checks the first element.
   * @param tag          Tag to check
   * @param elementType  Element type from {@link Tag}
   * @return  True if the tag is a list of the given type
   */
  public static boolean isListOf(@Nullable Tag tag, int elementType) {
    return tag instanceof ListTag list && (list.isEmpty() || isType(list.get(0), elementType));
  }

  /**
   * Checks if the given tag is a non-empty list of the given element type, matching the old {@code ListTag#getElementType() == type} check
   * @param tag          Tag to check
   * @param elementType  Element type from {@link Tag}
   * @return  True if the tag is a non-empty list of the given type
   */
  public static boolean isNonEmptyListOf(@Nullable Tag tag, int elementType) {
    return tag instanceof ListTag list && !list.isEmpty() && isType(list.get(0), elementType);
  }

  /**
   * Gets a list of the given element type, replacement for the removed {@code CompoundTag#getList(String, int)}.
   * Returns the live list if present with the right type, or a new empty list (not attached to the compound) otherwise.
   * @param nbt          Compound to read
   * @param key          List key
   * @param elementType  Element type from {@link Tag}
   * @return  List value
   */
  public static ListTag getList(CompoundTag nbt, String key, int elementType) {
    Tag tag = nbt.get(key);
    if (isListOf(tag, elementType)) {
      return (ListTag) tag;
    }
    return new ListTag();
  }
}
