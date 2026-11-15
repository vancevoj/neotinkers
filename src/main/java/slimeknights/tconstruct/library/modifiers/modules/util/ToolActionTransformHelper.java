package slimeknights.tconstruct.library.modifiers.modules.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.core.component.BlockTransformer.BlockTransformData;
import net.minecraft.core.component.BlockTransformer.TransformParticle;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.DataMapHooks;
import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Helper to transform blocks using a tool action ({@link ItemAbility}), for modules such as {@link slimeknights.tconstruct.library.modifiers.modules.behavior.ToolActionTransformModule}.
 * <p>
 * NeoForge 26.3 removed the strip, scrape, wax off, flatten and till item abilities. Vanilla tools now transform blocks through the
 * {@link BlockTransformer} registry ({@link BlockTransformers}) plus the NeoForge transformer data maps. This helper first asks the block,
 * as mod blocks and {@code BlockToolModificationEvent} listeners still answer item abilities, then falls back to the block transformer
 * that replaced the legacy ability.
 */
public final class ToolActionTransformHelper {
  private ToolActionTransformHelper() {}

  /** Legacy ability to strip a log, handled by the {@link BlockTransformers#AXE} transformer */
  public static final ItemAbility AXE_STRIP = ItemAbility.get("axe_strip");
  /** Legacy ability to scrape oxidization off copper, handled by the {@link BlockTransformers#AXE} transformer */
  public static final ItemAbility AXE_SCRAPE = ItemAbility.get("axe_scrape");
  /** Legacy ability to remove wax from copper, handled by the {@link BlockTransformers#AXE} transformer */
  public static final ItemAbility AXE_WAX_OFF = ItemAbility.get("axe_wax_off");
  /** Legacy ability to make a path, handled by the {@link BlockTransformers#SHOVEL} transformer */
  public static final ItemAbility SHOVEL_FLATTEN = ItemAbility.get("shovel_flatten");
  /** Legacy ability to till farmland, handled by the {@link BlockTransformers#HOE} transformer */
  public static final ItemAbility HOE_TILL = ItemAbility.get("hoe_till");

  /**
   * Result of a block transform
   * @param state  Transformed block state
   * @param data   Block transformer entry producing the state, null if the block or an event produced the state
   */
  public record Transform(BlockState state, @Nullable BlockTransformData data) {
    /**
     * Drops the loot of the block transformer entry, same as vanilla tools (e.g. hanging roots from tilling rooted dirt). Call before replacing the block.
     * @param context   Context of the transform, the clicked position is the transformed block
     * @param original  Block state before the transform
     */
    public void dropLoot(UseOnContext context, BlockState original) {
      if (data != null && data.loot().isPresent() && context.getLevel() instanceof ServerLevel level) {
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Block.dropFromBlockInteractLootTable(level, data.loot().get(), pos, original, level.getBlockEntity(pos), context.getItemInHand(), context.getPlayer(),
                                             (serverLevel, stack) -> data.dropStrategy().pop(serverLevel, pos, face, stack));
      }
    }
  }

  /** Gets the vanilla block transformer replacing the given legacy item ability, or null if the ability has no transformer */
  @Nullable
  public static ResourceKey<BlockTransformer> getTransformerKey(ItemAbility action) {
    if (action == AXE_STRIP || action == AXE_SCRAPE || action == AXE_WAX_OFF) {
      return BlockTransformers.AXE;
    }
    if (action == SHOVEL_FLATTEN) {
      return BlockTransformers.SHOVEL;
    }
    if (action == HOE_TILL) {
      return BlockTransformers.HOE;
    }
    return null;
  }

  /** Checks if the transformer entry belongs to the given ability. The axe transformer mixes stripping, scraping and waxing off, which are told apart by their particle. */
  private static boolean matchesAction(ItemAbility action, BlockTransformData data) {
    if (action == AXE_STRIP) {
      return data.particle() == TransformParticle.NONE;
    }
    if (action == AXE_SCRAPE) {
      return data.particle() == TransformParticle.SCRAPE;
    }
    if (action == AXE_WAX_OFF) {
      return data.particle() == TransformParticle.WAX_OFF;
    }
    return true;
  }

  /**
   * Finds the result of transforming the block at the context's clicked position with the given action
   * @param context   Use context, the clicked position is the block to transform
   * @param original  Block state at the clicked position
   * @param action    Action to perform
   * @param simulate  If true, the block is not allowed to make changes to the world. Transformer entries never change the world here.
   * @return  Transform result, or null if the block cannot be transformed
   */
  @Nullable
  public static Transform getTransform(UseOnContext context, BlockState original, ItemAbility action, boolean simulate) {
    // blocks and the NeoForge event still get the first say, this also covers the abilities NeoForge still handles (shears trim, firestarter light)
    BlockState transformed = original.getToolModifiedState(context, action, simulate);
    if (transformed != null) {
      return new Transform(transformed, null);
    }
    // fallback to the block transformer replacing the legacy ability
    ResourceKey<BlockTransformer> key = getTransformerKey(action);
    if (key == null) {
      return null;
    }
    Level level = context.getLevel();
    Optional<? extends Holder<BlockTransformer>> transformer = level.registryAccess().lookupOrThrow(Registries.BLOCK_TRANSFORMER).get(key);
    if (transformer.isEmpty()) {
      return null;
    }
    BlockPos pos = context.getClickedPos();
    Direction face = context.getClickedFace();
    for (BlockTransformData data : DataMapHooks.getAllTransformers(transformer.get())) {
      if (matchesAction(action, data) && !data.disallowedFaces().contains(face)) {
        BlockState state = data.blockStateProvider().value().getOptionalState(level, level.getRandom(), pos);
        if (state != null) {
          return new Transform(data.updateFromNeighbors() ? Block.updateFromNeighbourShapes(state, level, pos) : state, data);
        }
      }
    }
    return null;
  }

  /**
   * Gets the state from transforming the block at the context's clicked position with the given action
   * @see #getTransform(UseOnContext, BlockState, ItemAbility, boolean)
   */
  @Nullable
  public static BlockState getTransformedState(UseOnContext context, BlockState original, ItemAbility action, boolean simulate) {
    Transform transform = getTransform(context, original, action, simulate);
    return transform == null ? null : transform.state();
  }
}
