package slimeknights.tconstruct.world.worldgen.trees.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

/**
 * Huge fungus settings that replace the ground state with a ground tag.
 * The vanilla huge fungus configuration is gone (the vanilla feature is a final record now), so this holds all the settings used by
 * {@link slimeknights.tconstruct.world.worldgen.trees.feature.SlimeFungusFeature}.
 * @param groundTag          Tag of blocks the fungus may grow on
 * @param stemState          Stem block
 * @param hatState           Hat block
 * @param decorState         Decoration block placed in the hat
 * @param replaceableBlocks  Blocks the stem may replace
 * @param planted            If true, the fungus was grown from a sapling
 */
public record SlimeFungusConfig(TagKey<Block> groundTag, BlockState stemState, BlockState hatState, BlockState decorState, BlockPredicate replaceableBlocks, boolean planted) {
  public static final MapCodec<SlimeFungusConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
    TagKey.codec(Registries.BLOCK).fieldOf("valid_base").forGetter(SlimeFungusConfig::groundTag),
    BlockState.CODEC.fieldOf("stem_state").forGetter(SlimeFungusConfig::stemState),
    BlockState.CODEC.fieldOf("hat_state").forGetter(SlimeFungusConfig::hatState),
    BlockState.CODEC.fieldOf("decor_state").forGetter(SlimeFungusConfig::decorState),
    BlockPredicate.CODEC.fieldOf("replaceable_blocks").forGetter(SlimeFungusConfig::replaceableBlocks),
    Codec.BOOL.fieldOf("planted").orElse(false).forGetter(SlimeFungusConfig::planted)
  ).apply(instance, SlimeFungusConfig::new));
}
