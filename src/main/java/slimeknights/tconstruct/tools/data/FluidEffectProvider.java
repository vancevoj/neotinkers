package slimeknights.tconstruct.tools.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.fluids.FluidType;
import slimeknights.mantle.data.predicate.entity.LivingEntityPredicate;
import slimeknights.mantle.data.predicate.entity.MobTypePredicate;
import slimeknights.mantle.recipe.data.FluidNameIngredient;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.data.tinkering.AbstractFluidEffectProvider;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.TimeAction;
import slimeknights.tconstruct.library.modifiers.fluid.entity.AddBreathFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.AwardStatFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.CureEffectsFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.DamageFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.FireFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.FreezeFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.MobEffectFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.PotionFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.RemoveEffectFluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.entity.RestoreHungerFluidEffect;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.library.recipe.TagPredicate;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.traits.skull.StrongBonesModifier;

import java.util.function.Function;

public class FluidEffectProvider extends AbstractFluidEffectProvider {
  public FluidEffectProvider(DataGenerator generator) {
    super(generator, TConstruct.MOD_ID);
  }

  @Override
  protected void addFluids() {
    // vanilla
    addFluid(Fluids.WATER, FluidType.BUCKET_VOLUME / 20)
      .addEntityEffect(LivingEntityPredicate.WATER_SENSITIVE, new DamageFluidEffect(2f, DamageFluidEffect.BYPASS_ARMOR))
      .addEntityEffect(FluidEffect.EXTINGUISH_FIRE);
    addFluid(Fluids.LAVA, FluidType.BUCKET_VOLUME / 20)
      .addEntityEffect(LivingEntityPredicate.FIRE_IMMUNE.inverted(), new DamageFluidEffect(2f, DamageFluidEffect.FIRE))
      .addEntityEffect(new FireFluidEffect(TimeAction.SET, 10));
    addFluid(Tags.Fluids.MILK, FluidType.BUCKET_VOLUME / 10)
      .addEntityEffect(new CureEffectsFluidEffect(Items.MILK_BUCKET))
      .addEntityEffect(StrongBonesModifier.FLUID_EFFECT);
    addFluid(TinkerFluids.powderedSnow.getForgeTag(), FluidType.BUCKET_VOLUME / 10)
      .addEntityEffect(new FreezeFluidEffect(TimeAction.ADD, 160));

    // blaze - more damage, less fire
    burningFluid("blazing_blood", TinkerFluids.blazingBlood.getLocalTag(), FluidType.BUCKET_VOLUME / 20, 3f, 5);

    // slime
    int slimeballPiece = FluidValues.SLIMEBALL / 5;
    // earth - lucky
    addFluid(TinkerFluids.earthSlime.getForgeTag(), slimeballPiece)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.LUCK).timeSeconds(15).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SLOWDOWN).timeSeconds(15).build());
    // sky - jump boost
    addFluid(TinkerFluids.skySlime.getLocalTag(), slimeballPiece)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.JUMP).timeSeconds(20).level(1).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SLOWDOWN).timeSeconds(15).build());
    // ender - levitation
    addFluid(TinkerFluids.enderSlime.getLocalTag(), slimeballPiece)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.LEVITATION).timeSeconds(5).level(1).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SLOWDOWN).timeSeconds(15).build());
    // slimelike
    // venom - poison & strength
    addFluid(TinkerFluids.venom.getLocalTag(), slimeballPiece)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.POISON).timeSeconds(5).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.DAMAGE_BOOST).timeSeconds(10).build());
    // magma - fire resistance
    addFluid(TinkerFluids.magma.getForgeTag(), slimeballPiece)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.FIRE_RESISTANCE).timeSeconds(25).build());
    // soul - slowness and blindness
    addFluid(TinkerFluids.liquidSoul.getLocalTag(), FluidType.BUCKET_VOLUME / 20)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SLOWDOWN).timeSeconds(25).level(2).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.BLINDNESS).timeSeconds(5).build());
    // ender - teleporting
    addFluid(TinkerFluids.moltenEnder.getForgeTag(), FluidType.BUCKET_VOLUME / 20)
      .addEntityEffect(new DamageFluidEffect(1f, DamageFluidEffect.MAGIC))
      .addEntityEffect(FluidEffect.TELEPORT);

    // foods - setup to give equivelent saturation on a full bowl/bottle to their food counterparts, though hunger may be slightly different
    addFluid(TinkerFluids.honey.getForgeTag(), slimeballPiece)
      .addEntityEffect(new RestoreHungerFluidEffect(1, 0.12f, false, ItemOutput.fromItem(Items.HONEY_BOTTLE)))
      .addEntityEffect(new RemoveEffectFluidEffect(MobEffects.POISON));
    // soups
    int bowlSip = FluidValues.BOWL / 5;
    addFluid(TinkerFluids.beetrootSoup.getForgeTag(), bowlSip)
      .addEntityEffect(new RestoreHungerFluidEffect(1, 0.72f, false, ItemOutput.fromItem(Items.BEETROOT_SOUP)));
    addFluid(TinkerFluids.mushroomStew.getForgeTag(), bowlSip)
      .addEntityEffect(new RestoreHungerFluidEffect(1, 0.72f, false, ItemOutput.fromItem(Items.MUSHROOM_STEW)));
    addFluid(TinkerFluids.rabbitStew.getForgeTag(), bowlSip)
      .addEntityEffect(new RestoreHungerFluidEffect(2, 0.6f, false, ItemOutput.fromItem(Items.RABBIT_STEW)));
    addFluid(TinkerFluids.meatSoup.getLocalTag(), bowlSip)
      .addEntityEffect(new RestoreHungerFluidEffect(2, 0.48f, false, ItemOutput.fromItem(TinkerFluids.meatSoupBowl)));
    // pig iron fills you up food, but still hurts
    addFluid(TinkerFluids.moltenPigIron.getLocalTag(), FluidValues.NUGGET)
      .addEntityEffect(new RestoreHungerFluidEffect(2, 0.7f, false, ItemOutput.fromItem(TinkerCommons.bacon)))
      .addEntityEffect(new FireFluidEffect(TimeAction.SET, 2));

    // metals, lose reference to mistborn (though a true fan would probably get angry at how much I stray from the source)
    metalborn(TinkerFluids.moltenIron.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(TinkerModifiers.magneticEffect.get()).timeSeconds(4).level(2).build());
    metalborn(TinkerFluids.moltenSteel.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(TinkerModifiers.repulsiveEffect.get()).timeSeconds(4).level(2).build());
    metalborn(TinkerFluids.moltenCopper.getForgeTag(), 1.5f).addEntityEffect(new AddBreathFluidEffect(80));
    metalborn(TinkerFluids.moltenBronze.getForgeTag(), 2f).addEntityEffect(new AwardStatFluidEffect(Stats.TIME_SINCE_REST,  - 2000));
    metalborn(TinkerFluids.moltenAmethystBronze.getLocalTag(), 1.5f).addEntityEffect(new AwardStatFluidEffect(Stats.TIME_SINCE_REST, 2000));
    metalborn(TinkerFluids.moltenZinc.getForgeTag(), 1.5f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SPEED).timeSeconds(10).level(1).build());
    metalborn(TinkerFluids.moltenBrass.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.FIRE_RESISTANCE).timeSeconds(8).level(1).build());
    metalborn(TinkerFluids.moltenTin.getForgeTag(), 1.5f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.NIGHT_VISION).timeSeconds(8).level(1).build());
    metalborn(TinkerFluids.moltenPewter.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.DAMAGE_BOOST).timeSeconds(7).level(1).build());
    addFluid(TinkerFluids.moltenGold.getForgeTag(), FluidValues.NUGGET)
      .addEntityEffect(new MobTypePredicate(MobType.UNDEAD), new DamageFluidEffect(2f, DamageFluidEffect.MAGIC))
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.REGENERATION).timeSeconds(6).level(1).build());
    addFluid(TinkerFluids.moltenElectrum.getForgeTag(), FluidValues.NUGGET)
      .addEntityEffect(new MobTypePredicate(MobType.UNDEAD), new DamageFluidEffect(2f, DamageFluidEffect.MAGIC))
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.DIG_SPEED).timeSeconds(8).level(1).build());
    addFluid(TinkerFluids.moltenRoseGold.getForgeTag(), FluidValues.NUGGET)
      .addEntityEffect(new MobTypePredicate(MobType.UNDEAD), new DamageFluidEffect(2f, DamageFluidEffect.MAGIC))
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.HEALTH_BOOST).timeSeconds(15).level(1).build());
    metalborn(TinkerFluids.moltenAluminum.getForgeTag(), 1f).addEntityEffect(new CureEffectsFluidEffect(Items.MILK_BUCKET));
    addFluid(TinkerFluids.moltenSilver.getForgeTag(), FluidValues.NUGGET)
      .addEntityEffect(new MobTypePredicate(MobType.UNDEAD), new DamageFluidEffect(2f, DamageFluidEffect.MAGIC))
      .addEntityEffect(new RemoveEffectFluidEffect(MobEffects.WITHER));

    metalborn(TinkerFluids.moltenLead.getForgeTag(), 1.5f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SLOWDOWN).timeSeconds(6).level(1).build());
    metalborn(TinkerFluids.moltenNickel.getForgeTag(), 1.5f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.WEAKNESS).timeSeconds(7).level(1).build());
    metalborn(TinkerFluids.moltenInvar.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.HUNGER).timeSeconds(10).level(1).build());
    metalborn(TinkerFluids.moltenConstantan.getForgeTag(), 2f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.HUNGER).timeSeconds(10).level(1).build());
    burningFluid(TinkerFluids.moltenUranium.getForgeTag(), 1.5f, 3).addEntityEffect(MobEffectFluidEffect.set(MobEffects.POISON).timeSeconds(10).level(1).build());

    metalborn(TinkerFluids.moltenCobalt.getForgeTag(), 1f)
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.DIG_SPEED).timeSeconds(7).level(1).build())
      .addEntityEffect(MobEffectFluidEffect.set(MobEffects.MOVEMENT_SPEED).timeSeconds(7).level(1).build());
    metalborn(TinkerFluids.moltenManyullyn.getForgeTag(), 3f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.DAMAGE_RESISTANCE).timeSeconds(15).level(1).build());
    metalborn(TinkerFluids.moltenHepatizon.getForgeTag(), 2.5f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.DAMAGE_RESISTANCE).timeSeconds(10).level(1).build());
    burningFluid(TinkerFluids.moltenNetherite.getForgeTag(), 3.5f, 4).addEntityEffect(MobEffectFluidEffect.set(MobEffects.BLINDNESS).timeSeconds(15).level(1).build());

    metalborn(TinkerFluids.moltenSlimesteel.getLocalTag(), 1f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.SLOW_FALLING).timeSeconds(5).level(1).build());
    metalborn(TinkerFluids.moltenQueensSlime.getLocalTag(), 1f).addEntityEffect(MobEffectFluidEffect.set(MobEffects.LEVITATION).timeSeconds(5).level(1).build());

    // multi-recipes
    burningFluid("glass",           TinkerTags.Fluids.GLASS_SPILLING,           FluidType.BUCKET_VOLUME / 10, 1f,   3);
    burningFluid("clay",            TinkerTags.Fluids.CLAY_SPILLING,            FluidValues.BRICK / 5,        1.5f, 3);
    burningFluid("metal_cheap",     TinkerTags.Fluids.CHEAP_METAL_SPILLING,     FluidValues.NUGGET,           1.5f, 7);
    burningFluid("metal_average",   TinkerTags.Fluids.AVERAGE_METAL_SPILLING,   FluidValues.NUGGET,           2f,   7);
    burningFluid("metal_expensive", TinkerTags.Fluids.EXPENSIVE_METAL_SPILLING, FluidValues.NUGGET,           3f,   7);

    // potion fluid compat
    // standard potion is 250 mb, but we want a smaller number. divide into 5 pieces at 25% a piece (so healing is 1 health), means you gain 25% per potion
    int bottleSip = FluidValues.BOTTLE / 5;
    addFluid("potion_fluid", TinkerFluids.potion.getForgeTag(), bottleSip).addEntityEffect(new PotionFluidEffect(0.25f, TagPredicate.ANY));

    // create has three types of bottles stored on their fluid, react to it to boost
    Function<String,TagPredicate> createBottle = value -> {
      CompoundTag compound = new CompoundTag();
      compound.putString("Bottle", value);
      return new TagPredicate(compound);
    };
    String create = "create";
    addFluid("potion_create", FluidNameIngredient.of(new ResourceLocation(create, "potion"), bottleSip))
      .addCondition(new ModLoadedCondition(create))
      .addEntityEffect(new PotionFluidEffect(0.25f, createBottle.apply("REGULAR")))
      .addEntityEffect(new PotionFluidEffect(0.5f, createBottle.apply("SPLASH")))
      .addEntityEffect(new PotionFluidEffect(0.75f, createBottle.apply("LINGERING")));

  }

  /** Builder for an effect based metal */
  private Builder metalborn(TagKey<Fluid> tag, float damage) {
    return burningFluid(tag.location().getPath(), tag, FluidValues.NUGGET, damage, 0);
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Spilling Fluid Provider";
  }
}
