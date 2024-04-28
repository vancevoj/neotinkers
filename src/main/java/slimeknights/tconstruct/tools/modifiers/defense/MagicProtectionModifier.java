package slimeknights.tconstruct.tools.modifiers.defense;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.data.ModifierMaxLevel;
import slimeknights.tconstruct.library.modifiers.modules.armor.ProtectionModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.ComputableDataKey;

public class MagicProtectionModifier extends AbstractProtectionModifier<ModifierMaxLevel> {
  /** Entity data key for the data associated with this modifier */
  private static final ComputableDataKey<ModifierMaxLevel> MAGIC_DATA = TConstruct.createKey("magic_protection", ModifierMaxLevel::new);
  public MagicProtectionModifier() {
    super(MAGIC_DATA);
    // TODO: extract to data key module using ModifierEvents
    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, MobEffectEvent.Added.class, MagicProtectionModifier::onPotionStart);
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ProtectionModule.source(DamageSourcePredicate.CAN_PROTECT, DamageSourcePredicate.MAGIC).eachLevel(2.5f));
  }

  private static void onPotionStart(MobEffectEvent.Added event) {
    MobEffectInstance newEffect = event.getEffectInstance();
    if (!newEffect.getEffect().isBeneficial() && !newEffect.getCurativeItems().isEmpty()) {
      LivingEntity living = event.getEntity();
      living.getCapability(TinkerDataCapability.CAPABILITY).ifPresent(data -> {
        ModifierMaxLevel magicData = data.get(MAGIC_DATA);
        if (magicData != null) {
          float max = magicData.getMax();
          if (max > 0) {
            // decrease duration by 5% per level
            int duration = (int)(newEffect.getDuration() * (1 - (max * 0.05f)));
            if (duration < 0) {
              duration = 0;
            }
            newEffect.duration = duration;
          }
        }
      });
    }
  }
}
