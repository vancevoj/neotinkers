package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileLaunchModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition.ConditionalModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

import javax.annotation.Nullable;
import java.util.List;

/** Module that fills capacity by launching projectiles. */
public record LaunchCapacityModule(LevelingInt grant, @Nullable ModifierId owner, ModifierCondition<IToolStackView> condition) implements ModifierModule, ProjectileLaunchModifierHook, CapacitySourceModule, ConditionalModule<IToolStackView> {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<LaunchCapacityModule>defaultHooks(ModifierHooks.PROJECTILE_LAUNCH);
  public static final RecordLoadable<LaunchCapacityModule> LOADER = RecordLoadable.create(
    LevelingInt.LOADABLE.defaultField("grant", LevelingInt.ZERO, false, LaunchCapacityModule::grant),
    OWNER_FIELD, ModifierCondition.TOOL_FIELD,
    LaunchCapacityModule::new);

  @Override
  public RecordLoadable<LaunchCapacityModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }

  @Override
  public void onProjectileLaunch(IToolStackView tool, ModifierEntry modifier, LivingEntity shooter, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData, boolean primary) {
    CapacitySourceModule.apply(tool, barModifier(tool, modifier), 1, grant.compute(modifier.getEffectiveLevel()));
  }
}
