package slimeknights.tconstruct.tools.data;

import com.google.common.collect.ImmutableMap;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ToolActions;
import slimeknights.mantle.data.predicate.block.BlockPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.data.tinkering.AbstractToolDefinitionDataProvider;
import slimeknights.tconstruct.library.json.predicate.modifier.SingleModifierPredicate;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.definition.module.ToolModule;
import slimeknights.tconstruct.library.tools.definition.module.aoe.BoxAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.CircleAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.ConditionalAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.IBoxExpansion;
import slimeknights.tconstruct.library.tools.definition.module.aoe.TreeAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.aoe.VeiningAOEIterator;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolActionsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolSlotsModule;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolTraitsModule;
import slimeknights.tconstruct.library.tools.definition.module.interaction.DualOptionInteraction;
import slimeknights.tconstruct.library.tools.definition.module.interaction.PreferenceSetInteraction;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveModule;
import slimeknights.tconstruct.library.tools.definition.module.mining.MaxTierHarvestLogic;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedModifierModule;
import slimeknights.tconstruct.library.tools.definition.module.weapon.CircleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.ParticleWeaponAttack;
import slimeknights.tconstruct.library.tools.definition.module.weapon.SweepWeaponAttack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.ArmorDefinitions;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.TinkerToolActions;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.ToolDefinitions;
import slimeknights.tconstruct.tools.item.ArmorSlotType;
import slimeknights.tconstruct.tools.stats.SkullStats;

import static slimeknights.tconstruct.tools.TinkerToolParts.bowGrip;
import static slimeknights.tconstruct.tools.TinkerToolParts.bowLimb;
import static slimeknights.tconstruct.tools.TinkerToolParts.bowstring;
import static slimeknights.tconstruct.tools.TinkerToolParts.broadAxeHead;
import static slimeknights.tconstruct.tools.TinkerToolParts.broadBlade;
import static slimeknights.tconstruct.tools.TinkerToolParts.hammerHead;
import static slimeknights.tconstruct.tools.TinkerToolParts.largePlate;
import static slimeknights.tconstruct.tools.TinkerToolParts.pickHead;
import static slimeknights.tconstruct.tools.TinkerToolParts.roundPlate;
import static slimeknights.tconstruct.tools.TinkerToolParts.smallAxeHead;
import static slimeknights.tconstruct.tools.TinkerToolParts.smallBlade;
import static slimeknights.tconstruct.tools.TinkerToolParts.toolBinding;
import static slimeknights.tconstruct.tools.TinkerToolParts.toolHandle;
import static slimeknights.tconstruct.tools.TinkerToolParts.toughHandle;

public class ToolDefinitionDataProvider extends AbstractToolDefinitionDataProvider {
  public ToolDefinitionDataProvider(DataGenerator generator) {
    super(generator, TConstruct.MOD_ID);
  }

  @Override
  protected void addToolDefinitions() {
    // pickaxes
    define(ToolDefinitions.PICKAXE)
      // parts
      .part(pickHead)
      .part(toolHandle)
      .part(toolBinding)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 0.5f) // gains +0.5 damage from tool piercing, hence being lower than vanilla
      .stat(ToolStats.ATTACK_SPEED, 1.2f)
      .smallToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.pierce, 1).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.PICKAXE_DIG))
      .module(IsEffectiveModule.tag(BlockTags.MINEABLE_WITH_PICKAXE))
      .module(BoxAOEIterator.builder(0, 0, 0).addDepth(2).addHeight(1).direction(IBoxExpansion.PITCH).build());

    define(ToolDefinitions.SLEDGE_HAMMER)
      // parts
      .part(hammerHead, 2)
      .part(toughHandle)
      .part(largePlate, 1)
      .part(largePlate, 1)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 3f) // gains +5 undead damage from smite modifier
      .stat(ToolStats.ATTACK_SPEED, 0.75f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.35f)
      .multiplier(ToolStats.MINING_SPEED, 0.4f)
      .multiplier(ToolStats.DURABILITY, 4f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.smite, 2).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.PICKAXE_DIG))
      .module(IsEffectiveModule.tag(BlockTags.MINEABLE_WITH_PICKAXE))
      .module(BoxAOEIterator.builder(1, 1, 0).addWidth(1).addHeight(1).build())
      .module(new ParticleWeaponAttack(TinkerTools.hammerAttackParticle.get()));

    define(ToolDefinitions.VEIN_HAMMER)
      // parts
      .part(hammerHead, 2)
      .part(toughHandle)
      .part(pickHead, 1)
      .part(largePlate)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 3f) // gains +1.25 damage from piercing
      .stat(ToolStats.ATTACK_SPEED, 0.85f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.25f)
      .multiplier(ToolStats.MINING_SPEED, 0.3f)
      .multiplier(ToolStats.DURABILITY, 5.0f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.pierce, 2).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.PICKAXE_DIG))
      .module(IsEffectiveModule.tag(BlockTags.MINEABLE_WITH_PICKAXE))
      .module(new VeiningAOEIterator(2))
      .module(new ParticleWeaponAttack(TinkerTools.hammerAttackParticle.get()));


    // shovels
    define(ToolDefinitions.MATTOCK)
      // parts
      .part(smallAxeHead)
      .part(toolHandle)
      .part(roundPlate)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 1.5f)
      .stat(ToolStats.ATTACK_SPEED, 0.9f)
      .smallToolStartingSlots()
      .multiplier(ToolStats.DURABILITY, 1.25f)
      .multiplier(ToolStats.MINING_SPEED, 1.1f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.1f)
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.tilling).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.AXE_DIG, ToolActions.SHOVEL_DIG))
      .module(IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_MATTOCK))
      // 200% hand speed on any axe block we do not directly target
      .module(new MiningSpeedModifierModule(2f, BlockPredicate.and(BlockPredicate.tag(BlockTags.MINEABLE_WITH_AXE), BlockPredicate.tag(TinkerTags.Blocks.MINABLE_WITH_MATTOCK).inverted())))
      .module(new VeiningAOEIterator(0));

    define(ToolDefinitions.PICKADZE)
      // parts
      .part(pickHead)
      .part(toolHandle)
      .part(roundPlate)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 0.5f)
      .stat(ToolStats.ATTACK_SPEED, 1.3f)
      .smallToolStartingSlots()
      .multiplier(ToolStats.DURABILITY, 1.3f)
      .multiplier(ToolStats.MINING_SPEED, 0.75f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.15f)
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.pathing).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.PICKAXE_DIG, ToolActions.SHOVEL_DIG))
      .module(IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_PICKADZE))
      .module(new MaxTierHarvestLogic(Tiers.GOLD))
      .module(BoxAOEIterator.builder(0, 0, 0).addHeight(1).build());

    define(ToolDefinitions.EXCAVATOR)
      // parts
      .part(largePlate)
      .part(toughHandle)
      .part(largePlate)
      .part(toughHandle)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 1.5f)
      .stat(ToolStats.ATTACK_SPEED, 1.0f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.2f)
      .multiplier(ToolStats.MINING_SPEED, 0.3f)
      .multiplier(ToolStats.DURABILITY, 3.75f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder()
        .trait(TinkerModifiers.knockback, 2)
        .trait(ModifierIds.pathing).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.SHOVEL_DIG))
      .module(IsEffectiveModule.tag(BlockTags.MINEABLE_WITH_SHOVEL))
      .module(new ParticleWeaponAttack(TinkerTools.bonkAttackParticle.get()))
      .module(BoxAOEIterator.builder(1, 1, 0).addWidth(1).addHeight(1).build());


    // axes
    define(ToolDefinitions.HAND_AXE)
      // parts
      .part(smallAxeHead)
      .part(toolHandle)
      .part(toolBinding)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 6.0f)
      .stat(ToolStats.ATTACK_SPEED, 0.9f)
      .smallToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.stripping).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.AXE_DIG, TinkerToolActions.SHIELD_DISABLE))
      .module(IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_HAND_AXE))
      .module(new CircleAOEIterator(1, false))
      .module(new ParticleWeaponAttack(TinkerTools.axeAttackParticle.get()));

    define(ToolDefinitions.BROAD_AXE)
      // parts
      .part(broadAxeHead, 2)
      .part(toughHandle)
      .part(pickHead, 1)
      .part(toolBinding)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 5f)
      .stat(ToolStats.ATTACK_SPEED, 0.6f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.65f)
      .multiplier(ToolStats.MINING_SPEED, 0.3f)
      .multiplier(ToolStats.DURABILITY, 4.25f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(ModifierIds.stripping).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.AXE_DIG, TinkerToolActions.SHIELD_DISABLE))
      .module(IsEffectiveModule.tag(BlockTags.MINEABLE_WITH_AXE))
      .module(new ConditionalAOEIterator(
        BlockPredicate.tag(TinkerTags.Blocks.TREE_LOGS), new TreeAOEIterator(0, 0),
        BoxAOEIterator.builder(0, 5, 0).addWidth(1).addDepth(1).direction(IBoxExpansion.HEIGHT).build()))
      .module(new ParticleWeaponAttack(TinkerTools.axeAttackParticle.get()));

    // scythes
    ToolModule[] scytheHarvest = {
      IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_SCYTHE),
      MiningSpeedModifierModule.tag(BlockTags.WOOL, 0.3f),
      MiningSpeedModifierModule.blocks(0.10f, Blocks.VINE, Blocks.GLOW_LICHEN),
    };
    define(ToolDefinitions.KAMA)
      // parts
      .part(smallBlade)
      .part(toolHandle)
      .part(toolBinding)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 1f)
      .stat(ToolStats.ATTACK_SPEED, 1.6f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 0.5f)
      .smallToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder()
        .trait(ModifierIds.tilling)
        .trait(TinkerModifiers.shears)
        .trait(TinkerModifiers.harvest).build())
      // harvest
      .module(ToolActionsModule.of(ToolActions.HOE_DIG))
      .module(scytheHarvest)
      .module(new CircleAOEIterator(1, true))
      .module(new CircleWeaponAttack(1));

    define(ToolDefinitions.SCYTHE)
      // parts
      .part(TinkerToolParts.broadBlade)
      .part(TinkerToolParts.toughHandle)
      .part(TinkerToolParts.toolBinding)
      .part(TinkerToolParts.toughHandle)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 1f)
      .stat(ToolStats.ATTACK_SPEED, 0.7f)
      .multiplier(ToolStats.MINING_SPEED, 0.45f)
      .multiplier(ToolStats.DURABILITY, 2.5f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder()
        .trait(ModifierIds.tilling)
        .trait(TinkerModifiers.aoeSilkyShears)
        .trait(TinkerModifiers.harvest).build())
      // behavior
      .module(scytheHarvest)
      .module(BoxAOEIterator.builder(1, 1, 2).addExpansion(1, 1, 0).addDepth(2).build())
      .module(new CircleWeaponAttack(2));


    // swords
    define(ToolDefinitions.DAGGER)
      // parts
      .part(smallBlade)
      .part(toolHandle)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 3f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 0.65f)
      .stat(ToolStats.ATTACK_SPEED, 2.0f)
      .multiplier(ToolStats.MINING_SPEED, 0.75f)
      .multiplier(ToolStats.DURABILITY, 0.75f)
      .stat(ToolStats.BLOCK_AMOUNT, 10)
      .stat(ToolStats.USE_ITEM_SPEED, 1.0f)
      .smallToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder()
        .trait(TinkerModifiers.padded, 1)
        .trait(TinkerModifiers.offhandAttack)
        .trait(TinkerModifiers.silkyShears).build())
      // behavior
      .module(ToolActionsModule.of(ToolActions.SWORD_DIG, ToolActions.HOE_DIG))
      .module(IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_DAGGER))
      .module(MiningSpeedModifierModule.blocks(7.5f, Blocks.COBWEB));

    ToolModule[] swordHarvest = {
      IsEffectiveModule.tag(TinkerTags.Blocks.MINABLE_WITH_SWORD),
      MiningSpeedModifierModule.blocks(7.5f, Blocks.COBWEB),
      MiningSpeedModifierModule.blocks(100f, Blocks.BAMBOO, Blocks.BAMBOO_SAPLING)
    };
    define(ToolDefinitions.SWORD)
      // parts
      .part(smallBlade)
      .part(toolHandle)
      .part(toolHandle)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 3f)
      .stat(ToolStats.ATTACK_SPEED, 1.6f)
      .multiplier(ToolStats.MINING_SPEED, 0.5f)
      .multiplier(ToolStats.DURABILITY, 1.1f)
      .smallToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder().trait(TinkerModifiers.silkyShears).build())
      .module(ToolActionsModule.of(ToolActions.SWORD_DIG))
      // behavior
      .module(swordHarvest)
      .module(new SweepWeaponAttack(1));

    define(ToolDefinitions.CLEAVER)
      // parts
      .part(broadBlade)
      .part(toughHandle)
      .part(toughHandle)
      .part(largePlate)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 3f)
      .stat(ToolStats.ATTACK_SPEED, 1.0f)
      .multiplier(ToolStats.ATTACK_DAMAGE, 1.5f)
      .multiplier(ToolStats.MINING_SPEED, 0.25f)
      .multiplier(ToolStats.DURABILITY, 3.5f)
      .largeToolStartingSlots()
      // traits
      .module(ToolTraitsModule.builder()
        .trait(TinkerModifiers.severing, 2)
        .trait(TinkerModifiers.aoeSilkyShears).build())
      // behavior
      .module(ToolActionsModule.of(ToolActions.SWORD_DIG))
      .module(swordHarvest)
      .module(new SweepWeaponAttack(2));

    // bows
    define(ToolDefinitions.CROSSBOW)
      // parts
      .part(bowLimb)
      .part(bowGrip)
      .part(bowstring)
      // stats
      .stat(ToolStats.ATTACK_DAMAGE, 0f)
      .stat(ToolStats.ATTACK_SPEED, 1.0f)
      .multiplier(ToolStats.DURABILITY, 2f)
      .smallToolStartingSlots();
    define(ToolDefinitions.LONGBOW)
      // parts
      .part(bowLimb)
      .part(bowLimb)
      .part(bowGrip)
      .part(bowstring)
      // stats
      .stat(ToolStats.DURABILITY, 120)
      .stat(ToolStats.ATTACK_DAMAGE, 0f)
      .stat(ToolStats.ATTACK_SPEED, 1.0f)
      .multiplier(ToolStats.DURABILITY, 1.5f) // gets effectively 2x durability from having 2 heads
      .largeToolStartingSlots();

    // special
    define(ToolDefinitions.FLINT_AND_BRICK)
      // stats
      .stat(ToolStats.DURABILITY, 100)
      .module(new ToolSlotsModule(ImmutableMap.of(SlotType.UPGRADE, 1)))
      // traits
      .module(ToolTraitsModule.builder()
        .trait(TinkerModifiers.firestarter)
        .trait(TinkerModifiers.fiery)
        .trait(ModifierIds.scorching).build());
    // staff
    define(ToolDefinitions.SKY_STAFF)
      .stat(ToolStats.DURABILITY, 375)
      .stat(ToolStats.BLOCK_AMOUNT, 15)
      .stat(ToolStats.USE_ITEM_SPEED, 0.4f)
      .stat(ToolStats.VELOCITY, 0.8f)
      .stat(ToolStats.DRAW_SPEED, 1.25f)
      .module(ToolSlotsModule.builder()
        .slots(SlotType.UPGRADE, 5)
        .slots(SlotType.ABILITY, 2).build())
      .module(ToolTraitsModule.builder().trait(ModifierIds.overslimeFriend).build())
      .module(new CircleAOEIterator(1, false))
      .module(DualOptionInteraction.INSTANCE);
    define(ToolDefinitions.EARTH_STAFF)
      .stat(ToolStats.DURABILITY, 800)
      .stat(ToolStats.BLOCK_AMOUNT, 35)
      .stat(ToolStats.USE_ITEM_SPEED, 0.4f)
      .stat(ToolStats.PROJECTILE_DAMAGE, 1.5f)
      .stat(ToolStats.ACCURACY, 0.9f)
      .module(ToolSlotsModule.builder()
        .slots(SlotType.UPGRADE, 2)
        .slots(SlotType.DEFENSE, 3)
        .slots(SlotType.ABILITY, 2).build())
      .module(ToolTraitsModule.builder().trait(ModifierIds.overslimeFriend).build())
      .module(new CircleAOEIterator(1, false))
      .module(DualOptionInteraction.INSTANCE);
    define(ToolDefinitions.ICHOR_STAFF)
      .stat(ToolStats.DURABILITY, 1225)
      .stat(ToolStats.BLOCK_AMOUNT, 15)
      .stat(ToolStats.USE_ITEM_SPEED, 0.4f)
      .stat(ToolStats.VELOCITY, 1.2f)
      .stat(ToolStats.DRAW_SPEED, 0.75f)
      .module(ToolSlotsModule.builder()
        .slots(SlotType.UPGRADE, 2)
        .slots(SlotType.ABILITY, 3).build())
      .module(ToolTraitsModule.builder().trait(ModifierIds.overslimeFriend).build())
      .module(new CircleAOEIterator(1, false))
      .module(DualOptionInteraction.INSTANCE);
    define(ToolDefinitions.ENDER_STAFF)
      .stat(ToolStats.DURABILITY, 1520)
      .stat(ToolStats.BLOCK_AMOUNT, 15)
      .stat(ToolStats.BLOCK_ANGLE, 140)
      .stat(ToolStats.USE_ITEM_SPEED, 0.4f)
      .stat(ToolStats.PROJECTILE_DAMAGE, 3f)
      .stat(ToolStats.ACCURACY, 0.5f)
      .module(ToolSlotsModule.builder()
        .slots(SlotType.UPGRADE, 3)
        .slots(SlotType.DEFENSE, 1)
        .slots(SlotType.ABILITY, 2).build())
      .module(ToolTraitsModule.builder()
        .trait(ModifierIds.overslimeFriend)
        .trait(ModifierIds.reach, 2).build())
      .module(new CircleAOEIterator(1, false))
      .module(DualOptionInteraction.INSTANCE);


    // travelers armor
    ToolSlotsModule travelersSlots = ToolSlotsModule.builder()
      .slots(SlotType.UPGRADE, 3)
      .slots(SlotType.DEFENSE, 2)
      .slots(SlotType.ABILITY, 1).build();
    defineArmor(ArmorDefinitions.TRAVELERS)
      .durabilityFactor(10)
      .statEach(ToolStats.ARMOR, 1, 4, 5, 1)
      .multiplier(ArmorSlotType.CHESTPLATE, ToolStats.ATTACK_DAMAGE, 0.55f)
      .module(travelersSlots)
      .module(ArmorSlotType.BOOTS, ToolTraitsModule.builder().trait(ModifierIds.snowBoots).build());
    define(ArmorDefinitions.TRAVELERS_SHIELD)
      .stat(ToolStats.DURABILITY, 200)
      .stat(ToolStats.BLOCK_AMOUNT, 10)
      .stat(ToolStats.BLOCK_ANGLE, 90)
      .stat(ToolStats.USE_ITEM_SPEED, 0.8f)
      .module(travelersSlots)
      .module(ToolTraitsModule.builder().trait(TinkerModifiers.blocking).build())
      .module(new PreferenceSetInteraction(InteractionSource.RIGHT_CLICK, new SingleModifierPredicate(TinkerModifiers.blocking.getId())));

    // plate armor
    ToolSlotsModule plateSlots = ToolSlotsModule.builder()
      .slots(SlotType.UPGRADE, 1)
      .slots(SlotType.DEFENSE, 4)
      .slots(SlotType.ABILITY, 1).build();
    defineArmor(ArmorDefinitions.PLATE)
      .durabilityFactor(30)
      .statEach(ToolStats.ARMOR, 2, 5, 7, 2)
      .statAll(ToolStats.ARMOR_TOUGHNESS, 2)
      .statAll(ToolStats.KNOCKBACK_RESISTANCE, 0.1f)
      .multiplier(ArmorSlotType.CHESTPLATE, ToolStats.ATTACK_DAMAGE, 0.4f)
      .module(plateSlots);
    define(ArmorDefinitions.PLATE_SHIELD)
      .stat(ToolStats.DURABILITY, 500)
      .stat(ToolStats.BLOCK_AMOUNT, 100)
      .stat(ToolStats.BLOCK_ANGLE, 180)
      .stat(ToolStats.ARMOR_TOUGHNESS, 2)
      .module(plateSlots)
      .module(ToolTraitsModule.builder().trait(TinkerModifiers.blocking).build())
      .module(new PreferenceSetInteraction(InteractionSource.RIGHT_CLICK, new SingleModifierPredicate(TinkerModifiers.blocking.getId())));

    // slime suit
    ToolSlotsModule slimeSlots = ToolSlotsModule.builder()
      .slots(SlotType.UPGRADE, 5)
      .slots(SlotType.ABILITY, 1).build();
    ToolTraitsModule.Builder slimeTraits = ToolTraitsModule.builder().trait(ModifierIds.overslimeFriend);
    defineArmor(ArmorDefinitions.SLIMESUIT)
      .statEach(ToolStats.DURABILITY, 546, 630, 672, 362)
      .multiplier(ArmorSlotType.CHESTPLATE, ToolStats.ATTACK_DAMAGE, 0.4f)
      .module(slimeSlots)
      .part(ArmorSlotType.HELMET, SkullStats.ID, 1)
      .module(ArmorSlotType.HELMET, slimeTraits.build())
      .module(ArmorSlotType.CHESTPLATE, slimeTraits.copy().trait(ModifierIds.wings).build())
      .module(ArmorSlotType.LEGGINGS, slimeTraits.copy()
        .trait(ModifierIds.pockets, 1)
        .trait(TinkerModifiers.shulking, 1).build())
      .module(ArmorSlotType.BOOTS, slimeTraits.copy()
        .trait(TinkerModifiers.bouncy)
        .trait(TinkerModifiers.leaping, 1).build());
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Tool Definition Data Generator";
  }
}
