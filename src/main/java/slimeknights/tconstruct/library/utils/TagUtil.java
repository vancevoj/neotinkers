package slimeknights.tconstruct.library.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import slimeknights.mantle.client.SafeClientAccess;

import javax.annotation.Nullable;

/** Helpers related to Tag */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagUtil {
  /** Type matching any numeric tag in {@link #contains(CompoundTag, String, int)}, replaces the removed {@code Tag.TAG_ANY_NUMERIC} */
  public static final int TAG_ANY_NUMERIC = 99;
  /** Lookup of just the built in registries, used when no level is loaded. Initialized lazily to ensure the registries are populated */
  private static HolderLookup.Provider builtinRegistries;

  /* Helper functions */

  /**
   * Gets the registries for (de)serializing item and fluid stacks stored in NBT without access to a level, such as stacks inside tool data.
   * Stacks may reference data driven registries (enchantments, banner patterns, armor trims) that the built in registries lack, so this
   * returns the registries of the server or client level matching the current thread, falling back to the built in registries with no level.
   * @return  Registry lookup
   */
  public static HolderLookup.Provider registryAccess() {
    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    if (server != null && server.isSameThread()) {
      return server.registryAccess();
    }
    RegistryAccess client = SafeClientAccess.getRegistryAccess();
    if (client != null) {
      return client;
    }
    if (server != null) {
      return server.registryAccess();
    }
    if (builtinRegistries == null) {
      builtinRegistries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
    return builtinRegistries;
  }

  /**
   * Reads a block position from a single tag. Accepts the int array written by {@link BlockPos#CODEC} (and by {@code NbtUtils.writeBlockPos} in 1.21),
   * and the X/Y/Z compound written before 1.20.5 so positions saved by 1.20.1 worlds still load.
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
               && compound.get("X") instanceof NumericTag x && compound.get("Y") instanceof NumericTag y && compound.get("Z") instanceof NumericTag z) {
      return new BlockPos(x.intValue(), y.intValue(), z.intValue());
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
    // positions are written as an int array since 1.21 (a compound before), readPos accepts both
    BlockPos pos = readPos(parent.get(key));
    return pos != null ? pos.offset(offset) : null;
  }

  /**
   * Reads a block position from a value input, such as in block entity loading.
   * Reads the raw tag so both the 1.21 int array and the pre 1.20.5 compound forms load, see {@link #readPos(Tag)}.
   * @param input   Value input
   * @param key     Position key
   * @param offset  Amount to offset position by
   * @return  Block position, or null if invalid or missing
   */
  @Nullable
  public static BlockPos readOptionalPos(ValueInput input, String key, BlockPos offset) {
    BlockPos pos = readPos(input.read(key, ExtraCodecs.NBT).orElse(null));
    return pos != null ? pos.offset(offset) : null;
  }

  /**
   * Writes a block position relative to the given offset, in the same int array form read by {@link #readOptionalPos(CompoundTag, String, BlockPos)}.
   * @param parent  Parent tag
   * @param key     Position key
   * @param pos     Position to write
   * @param offset  Amount to subtract from the position
   */
  public static void writeOptionalPos(CompoundTag parent, String key, BlockPos pos, BlockPos offset) {
    BlockPos relative = pos.subtract(offset);
    parent.putIntArray(key, new int[] {relative.getX(), relative.getY(), relative.getZ()});
  }

  /**
   * Writes a block position relative to the given offset, in the same int array form read by {@link #readOptionalPos(ValueInput, String, BlockPos)}.
   * @param output  Value output
   * @param key     Position key
   * @param pos     Position to write
   * @param offset  Amount to subtract from the position
   */
  public static void writeOptionalPos(ValueOutput output, String key, BlockPos pos, BlockPos offset) {
    output.store(key, BlockPos.CODEC, pos.subtract(offset));
  }

  /**
   * Checks if the compound contains the key with the given tag type, replacing the removed {@code CompoundTag#contains(String, int)}.
   * @param tag   Compound to check
   * @param key   Key to check
   * @param type  Tag type, see {@link Tag} for values, or {@link #TAG_ANY_NUMERIC} for any number
   * @return  True if the key is present with the given type
   */
  public static boolean contains(CompoundTag tag, String key, int type) {
    Tag value = tag.get(key);
    if (value == null) {
      return false;
    }
    if (type == TAG_ANY_NUMERIC) {
      return value instanceof NumericTag;
    }
    return value.getId() == type;
  }

  /**
   * Gets a list from the compound if all its elements have the given type, replacing the removed {@code CompoundTag#getList(String, int)}.
   * Like the old method, returns the list in the compound (so changes write back), or a new empty list if missing or of another type.
   * @param tag          Compound to read
   * @param key          Key of the list
   * @param elementType  Tag type of the elements, see {@link Tag} for values
   * @return  List in the compound, or a new empty list
   */
  public static ListTag getList(CompoundTag tag, String key, int elementType) {
    if (tag.get(key) instanceof ListTag list) {
      for (Tag element : list) {
        if (element.getId() != elementType) {
          return new ListTag();
        }
      }
      return list;
    }
    return new ListTag();
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
