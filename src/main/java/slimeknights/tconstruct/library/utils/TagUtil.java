package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /* Helper functions */

  /**
   * Reads a block position from a single tag. Accepts the int array written by {@link NbtUtils#writeBlockPos(BlockPos)} in 1.21,
   * and the X/Y/Z compound it wrote before 1.20.5 so positions saved by 1.20.1 worlds still load.
   * @param tag  Tag to read
   * @return  Block position, or null if the tag is not a valid position
   */
  @Nullable
  public static BlockPos readPos(@Nullable Tag tag) {
    if (tag instanceof IntArrayTag array) {
      int[] coords = array.getAsIntArray();
      if (coords.length == 3) {
        return new BlockPos(coords[0], coords[1], coords[2]);
      }
    } else if (tag instanceof CompoundTag compound
               && compound.contains("X", Tag.TAG_ANY_NUMERIC) && compound.contains("Y", Tag.TAG_ANY_NUMERIC) && compound.contains("Z", Tag.TAG_ANY_NUMERIC)) {
      return new BlockPos(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
    }
    return null;
  }

  /**
   * Reads a block position from Tag
   * @param parent  Parent tag
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(CompoundTag parent, String key, BlockPos offset) {
    // NbtUtils.writeBlockPos writes an int array in 1.21 (a compound before), readPos accepts both
    BlockPos pos = readPos(parent.get(key));
    return pos != null ? pos.offset(offset) : null;
  }

  /**
   * Checks if the given tag is a numeric type
   * @param tag  Tag to check
   * @return  True if the type matches
   */
  public static boolean isNumeric(Tag tag) {
    byte type = tag.getId();
    return type == Tag.TAG_BYTE || type == Tag.TAG_SHORT || type == Tag.TAG_INT || type == Tag.TAG_LONG || type == Tag.TAG_FLOAT || type == Tag.TAG_DOUBLE;
  }
}
