package slimeknights.tconstruct.library.tools.definition.harvest;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Same as tag harvest, but applies additional modifiers to the break speed */
public class ModifiedHarvestLogic extends TagHarvestLogic {
  public static final RecordLoadable<ModifiedHarvestLogic> LOADER = RecordLoadable.create(
    TAG_FIELD, SpeedModifier.LOADABLE.list(1).requiredField("modifiers", l -> l.speedModifiers),
    ModifiedHarvestLogic::new);

  private final List<SpeedModifier> speedModifiers;
  protected ModifiedHarvestLogic(TagKey<Block> tag, List<SpeedModifier> speedModifiers) {
    super(tag);
    this.speedModifiers = speedModifiers;
  }

  /** Creates a builder for this logic */
  public static Builder builder(TagKey<Block> tag) {
    return new Builder(tag);
  }

  @Override
  public RecordLoadable<ModifiedHarvestLogic> getLoader() {
    return LOADER;
  }

  @Override
  public float getDestroySpeed(IToolStackView tool, BlockState state) {
    float speed = super.getDestroySpeed(tool, state);
    for (SpeedModifier modifier : speedModifiers) {
      if (modifier.predicate.matches(state)) {
        return Math.max(1, speed * modifier.modifier);
      }
    }
    return speed;
  }

  /** Builder for the logic */
  @SuppressWarnings("unused")
  @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Builder {
    private final TagKey<Block> tag;
    private final ImmutableList.Builder<SpeedModifier> speedModifiers = ImmutableList.builder();

    /** Base method to add a modifier */
    public Builder addModifier(float modifier, IJsonPredicate<BlockState> predicate) {
      speedModifiers.add(new SpeedModifier(modifier, predicate));
      return this;
    }

    /** Adds a modifier when the block matches a tag */
    public Builder tagModifier(TagKey<Block> tag, float modifier) {
      return addModifier(modifier, BlockPredicate.tag(tag));
    }

    /** Adds a modifier when the block does not match a tag */
    public Builder notTagModifier(TagKey<Block> tag, float modifier) {
      return addModifier(modifier, BlockPredicate.tag(tag).inverted());
    }

    /** Adds a modifier when the block matches a tag */
    public Builder blockModifier(float modifier, Block... blocks) {
      return addModifier(modifier, BlockPredicate.set(blocks));
    }

    /** Adds a modifier when the block matches a tag */
    public Builder notBlockModifier(float modifier, Block... blocks) {
      return addModifier(modifier, BlockPredicate.set(blocks).inverted());
    }

    /** Builds the modifier */
    public ModifiedHarvestLogic build() {
      return new ModifiedHarvestLogic(tag, speedModifiers.build());
    }
  }

  /** Speed modifier to apply to a block */
  private record SpeedModifier(float modifier, IJsonPredicate<BlockState> predicate) {
    public static final RecordLoadable<SpeedModifier> LOADABLE = RecordLoadable.create(
      FloatLoadable.ANY.requiredField("modifier", SpeedModifier::modifier),
      BlockPredicate.LOADER.requiredField("predicate", SpeedModifier::predicate),
      SpeedModifier::new);
  }
}
