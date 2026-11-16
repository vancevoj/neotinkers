package slimeknights.tconstruct.library.tools.definition.module.mining;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;

/** Module for adjusting the mining speed */
public record MiningSpeedModifierModule(float modifier, IJsonPredicate<BlockState> predicate) implements MiningSpeedToolHook, ToolModule {
  public static final RecordLoadable<MiningSpeedModifierModule> LOADER = RecordLoadable.create(
    FloatLoadable.ANY.requiredField("modifier", MiningSpeedModifierModule::modifier),
    BlockPredicate.LOADER.directField("predicate_type", MiningSpeedModifierModule::predicate),
    MiningSpeedModifierModule::new);
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<MiningSpeedModifierModule>defaultHooks(ToolHooks.MINING_SPEED);

  /** Modifies the given tag */
  public static MiningSpeedModifierModule tag(TagKey<Block> tag, float modifier) {
    return new MiningSpeedModifierModule(modifier, BlockPredicate.tag(tag));
  }

  /** Modifies the given blocks */
  public static MiningSpeedModifierModule blocks(float modifier, Block... blocks) {
    return new MiningSpeedModifierModule(modifier, BlockPredicate.set(blocks));
  }

  @Override
  public RecordLoadable<MiningSpeedModifierModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public float modifyDestroySpeed(IToolStackView tool, BlockState state, float speed) {
    if (predicate.matches(state)) {
      speed *= modifier;
    }
    return speed;
  }
}
