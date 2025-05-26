package slimeknights.tconstruct.tools.modifiers.upgrades.ranged;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

public class ScopeModifier extends Modifier implements EquipmentChangeModifierHook, UsingToolModifierHook {
  public static final ResourceLocation SCOPE = TConstruct.getResource("longbow_scope");

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addHook(this, ModifierHooks.TOOL_USING, ModifierHooks.EQUIPMENT_CHANGE);
  }

  @Override
  public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
    if (context.getLevel().isClientSide) {
      IToolStackView replacement = context.getReplacementTool();
      if (replacement == null || replacement.getModifierLevel(this) == 0) {
        context.getTinkerData().ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(SCOPE));
      }
    }
  }

  @Override
  public void onUsingTick(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int useDuration, int timeLeft, ModifierEntry activeModifier) {
    if (entity.level().isClientSide && tool.getModifierLevel(TinkerModifiers.scope.getId()) > 0) {
      int useTime = useDuration - timeLeft;
      if (useTime > 0) {
        float drawTime = tool.getPersistentData().getInt(GeneralInteractionModifierHook.KEY_DRAWTIME);
        if (drawTime <= 0) {
          drawTime = 20;
        }
        float fov = 1 - (0.6f * Math.min(useTime / drawTime, 1));
        entity.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).set(SCOPE, fov));
      }
    }
  }

  /** @deprecated no longer needed in modifiers. Call {@link UsingToolModifierHook#onUsingTick(IToolStackView, ModifierEntry, LivingEntity, int, int, ModifierEntry)} in tools. */
  @Deprecated(forRemoval = true)
  public static void scopingUsingTick(IToolStackView tool, LivingEntity entity, int chargeTime) {}

  /**
   * Cancels the scoping effect for the given entity.
   * @param entity  Entity
   * @deprecated No longer necessary to call in your modifier. For custom tools, see {@link UsingToolModifierHook#afterStopUsing(IToolStackView, LivingEntity, int)}
   */
  @Deprecated(forRemoval = true)
  public static void stopScoping(LivingEntity entity) {
    if (entity.level().isClientSide) {
      entity.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> data.computeIfAbsent(TinkerDataKeys.FOV_MODIFIER).remove(SCOPE));
    }
  }
}
