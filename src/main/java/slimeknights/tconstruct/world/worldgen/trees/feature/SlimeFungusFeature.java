package slimeknights.tconstruct.world.worldgen.trees.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import slimeknights.tconstruct.world.worldgen.trees.config.SlimeFungusConfig;

/**
 * Huge fungus that grows on a ground tag instead of a single ground block.
 * The vanilla huge fungus feature is a final record, so its stem and hat placement is reimplemented here.
 */
public record SlimeFungusFeature(SlimeFungusConfig config) implements Feature {
  public static final MapCodec<SlimeFungusFeature> CODEC = SlimeFungusConfig.CODEC.xmap(SlimeFungusFeature::new, SlimeFungusFeature::config);

  @Override
  public MapCodec<SlimeFungusFeature> codec() {
    return CODEC;
  }

  @Override
  public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos pos) {
    // must be on the right ground
    if (!level.getBlockState(pos.below()).is(config.groundTag())) {
      return false;
    }
    // ensure not too tall
    int height = Mth.nextInt(random, 4, 13);
    if (random.nextInt(12) == 0) {
      height *= 2;
    }
    if (!config.planted() && pos.getY() + height + 1 >= chunkGenerator.getGenDepth()) {
      return false;
    }
    // actual generation
    boolean flag = !config.planted() && random.nextFloat() < 0.06F;
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 4);
    this.placeStem(level, random, pos, height, flag);
    this.placeHat(level, random, pos, height, flag);
    return true;
  }


  /* Copy of the vanilla huge fungus logic, as the vanilla methods are private on a final record */

  /** Checks if the position may be replaced by the fungus */
  private boolean isReplaceable(WorldGenLevel level, BlockPos pos, boolean checkNonReplaceablePlants) {
    if (level.isStateAtPosition(pos, BlockBehaviour.BlockStateBase::canBeReplaced)) {
      return true;
    }
    return checkNonReplaceablePlants && config.replaceableBlocks().test(level, pos);
  }

  /** Places the fungus stem */
  private void placeStem(WorldGenLevel level, RandomSource random, BlockPos surfaceOrigin, int totalHeight, boolean isHuge) {
    BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    int stemRadius = isHuge ? 1 : 0;

    for (int dx = -stemRadius; dx <= stemRadius; dx++) {
      for (int dz = -stemRadius; dz <= stemRadius; dz++) {
        boolean cornerOfHugeStem = isHuge && Mth.abs(dx) == stemRadius && Mth.abs(dz) == stemRadius;

        for (int dy = 0; dy < totalHeight; dy++) {
          blockPos.setWithOffset(surfaceOrigin, dx, dy, dz);
          if (this.isReplaceable(level, blockPos, true)) {
            if (config.planted()) {
              if (!level.getBlockState(blockPos.below()).isAir()) {
                level.destroyBlock(blockPos, true);
              }

              level.setBlockAndUpdate(blockPos, config.stemState());
            } else if (cornerOfHugeStem) {
              if (random.nextFloat() < 0.1F) {
                this.setBlock(level, blockPos, config.stemState());
              }
            } else {
              this.setBlock(level, blockPos, config.stemState());
            }
          }
        }
      }
    }
  }

  /** Places the fungus hat */
  private void placeHat(WorldGenLevel level, RandomSource random, BlockPos surfaceOrigin, int totalHeight, boolean isHuge) {
    BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    boolean placeVines = config.hatState().is(Blocks.NETHER_WART_BLOCK);
    int hatHeight = Math.min(random.nextInt(1 + totalHeight / 3) + 5, totalHeight);
    int hatStartY = totalHeight - hatHeight;

    for (int dy = hatStartY; dy <= totalHeight; dy++) {
      int radius = dy < totalHeight - random.nextInt(3) ? 2 : 1;
      if (hatHeight > 8 && dy < hatStartY + 4) {
        radius = 3;
      }

      if (isHuge) {
        radius++;
      }

      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          boolean isEdgeX = dx == -radius || dx == radius;
          boolean isEdgeZ = dz == -radius || dz == radius;
          boolean inside = !isEdgeX && !isEdgeZ && dy != totalHeight;
          boolean corner = isEdgeX && isEdgeZ;
          boolean isHatBottom = dy < hatStartY + 3;
          blockPos.setWithOffset(surfaceOrigin, dx, dy, dz);
          if (this.isReplaceable(level, blockPos, false)) {
            if (config.planted() && !level.getBlockState(blockPos.below()).isAir()) {
              level.destroyBlock(blockPos, true);
            }

            if (isHatBottom) {
              if (!inside) {
                this.placeHatDropBlock(level, random, blockPos, placeVines);
              }
            } else if (inside) {
              this.placeHatBlock(level, random, blockPos, 0.1F, 0.2F, placeVines ? 0.1F : 0.0F);
            } else if (corner) {
              this.placeHatBlock(level, random, blockPos, 0.01F, 0.7F, placeVines ? 0.083F : 0.0F);
            } else {
              this.placeHatBlock(level, random, blockPos, 5.0E-4F, 0.98F, placeVines ? 0.07F : 0.0F);
            }
          }
        }
      }
    }
  }

  /** Places a single block in the hat */
  private void placeHatBlock(LevelAccessor level, RandomSource random, BlockPos.MutableBlockPos blockPos, float decorBlockProbability, float hatBlockProbability, float vinesProbability) {
    if (random.nextFloat() < decorBlockProbability) {
      this.setBlock(level, blockPos, config.decorState());
    } else if (random.nextFloat() < hatBlockProbability) {
      this.setBlock(level, blockPos, config.hatState());
      if (random.nextFloat() < vinesProbability) {
        tryPlaceWeepingVines(blockPos, level, random);
      }
    }
  }

  /** Places a block hanging from the bottom of the hat */
  private void placeHatDropBlock(LevelAccessor level, RandomSource random, BlockPos blockPos, boolean placeVines) {
    if (level.getBlockState(blockPos.below()).is(config.hatState().getBlock())) {
      this.setBlock(level, blockPos, config.hatState());
    } else if (random.nextFloat() < 0.15) {
      this.setBlock(level, blockPos, config.hatState());
      if (placeVines && random.nextInt(11) == 0) {
        tryPlaceWeepingVines(blockPos, level, random);
      }
    }
  }

  /** Attempts to place weeping vines below the hat */
  private static void tryPlaceWeepingVines(BlockPos hatBlockPos, LevelAccessor level, RandomSource random) {
    BlockPos.MutableBlockPos placePos = hatBlockPos.mutable().move(Direction.DOWN);
    if (level.isEmptyBlock(placePos)) {
      int goalVineHeight = Mth.nextInt(random, 1, 5);
      if (random.nextInt(7) == 0) {
        goalVineHeight *= 2;
      }
      placeWeepingVinesColumn(level, random, placePos, goalVineHeight, 23, 25);
    }
  }

  /** Places a column of weeping vines */
  private static void placeWeepingVinesColumn(LevelAccessor level, RandomSource random, BlockPos origin, int totalHeight, int minAge, int maxAge) {
    BlockPos.MutableBlockPos placePos = origin.mutable();

    for (int height = 0; height <= totalHeight; height++) {
      if (level.isEmptyBlock(placePos)) {
        if (height == totalHeight || !level.isEmptyBlock(placePos.below())) {
          level.setBlock(placePos, Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(random, minAge, maxAge)), 2);
          break;
        }

        level.setBlock(placePos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
      }

      placePos.move(Direction.DOWN);
    }
  }
}
