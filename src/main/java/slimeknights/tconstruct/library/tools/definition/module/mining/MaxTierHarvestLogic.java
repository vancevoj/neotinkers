package slimeknights.tconstruct.library.tools.definition.module.mining;

import net.minecraft.world.item.Tier;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.ModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierHookProvider;
import slimeknights.tconstruct.library.tools.definition.module.ToolHooks;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.utils.HarvestTiers;

import java.util.List;

/** Module that limits the tier to the given max */
public record MaxTierHarvestLogic(Tier tier) implements MiningTierToolHook, ToolModule {
  public static final RecordLoadable<MaxTierHarvestLogic> LOADER = RecordLoadable.create(TinkerLoadables.TIER.requiredField("tier", MaxTierHarvestLogic::tier), MaxTierHarvestLogic::new);
  private static final List<ModifierHook<?>> DEFAULT_HOOKS = ModifierHookProvider.<MaxTierHarvestLogic>defaultHooks(ToolHooks.MINING_TIER);

  @Override
  public RecordLoadable<MaxTierHarvestLogic> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModifierHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public Tier modifyTier(IToolStackView tool, Tier tier) {
    return HarvestTiers.min(this.tier, tier);
  }
}
