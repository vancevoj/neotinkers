package slimeknights.tconstruct.common.data;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

import static slimeknights.tconstruct.TConstruct.prefix;
import static slimeknights.tconstruct.common.TinkerDamageTypes.BLEEDING;
import static slimeknights.tconstruct.common.TinkerDamageTypes.MELEE_ARROW;
import static slimeknights.tconstruct.common.TinkerDamageTypes.PIERCING;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SELF_DESTRUCT;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_HEAT;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_MAGIC;

/** Datagen for damage types */
public class DamageTypeProvider implements RegistrySetBuilder.RegistryBootstrap<DamageType> {
  private DamageTypeProvider() {}

  /** Registers this provider with the registry set builder */
  public static void register(RegistrySetBuilder builder) {
    builder.add(Registries.DAMAGE_TYPE, new DamageTypeProvider());
  }

  @Override
  public void run(BootstapContext<DamageType> context) {
    context.register(SMELTERY_HEAT, new DamageType(prefix("smeltery_heat"), DamageScaling.NEVER, 0.1f, DamageEffects.BURNING));
    context.register(SMELTERY_MAGIC, new DamageType(prefix("smeltery_magic"), DamageScaling.NEVER, 0.1f, DamageEffects.BURNING));
    context.register(PIERCING, new DamageType(prefix("piercing"), 0.1f));
    context.register(BLEEDING, new DamageType(prefix("bleed"), DamageScaling.NEVER, 0.1f));
    context.register(SELF_DESTRUCT, new DamageType(prefix("self_destruct"), DamageScaling.NEVER, 0.1f));
    context.register(MELEE_ARROW, new DamageType("arrow", 0.1f));
  }
}
