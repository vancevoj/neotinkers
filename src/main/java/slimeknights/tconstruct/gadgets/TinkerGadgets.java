package slimeknights.tconstruct.gadgets;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock;
import slimeknights.tconstruct.gadgets.block.FoodCakeBlock.EffectCombination;
import slimeknights.tconstruct.gadgets.block.InvertedCakeBlock;
import slimeknights.tconstruct.gadgets.block.PunjiBlock;
import slimeknights.tconstruct.gadgets.capability.PiggybackCapability;
import slimeknights.tconstruct.gadgets.data.GadgetRecipeProvider;
import slimeknights.tconstruct.gadgets.entity.DispenseFancyArmorStand;
import slimeknights.tconstruct.gadgets.entity.EFLNEntity;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity.StandType;
import slimeknights.tconstruct.gadgets.entity.FancyItemFrameEntity;
import slimeknights.tconstruct.gadgets.entity.FrameType;
import slimeknights.tconstruct.gadgets.entity.GlowballEntity;
import slimeknights.tconstruct.gadgets.entity.shuriken.FlintShurikenEntity;
import slimeknights.tconstruct.gadgets.entity.shuriken.QuartzShurikenEntity;
import slimeknights.tconstruct.gadgets.item.EFLNItem;
import slimeknights.tconstruct.gadgets.item.FancyArmorStandItem;
import slimeknights.tconstruct.gadgets.item.FancyItemFrameItem;
import slimeknights.tconstruct.gadgets.item.FoodCakeItem;
import slimeknights.tconstruct.gadgets.item.GlowBallItem;
import slimeknights.tconstruct.gadgets.item.PiggyBackPackItem;
import slimeknights.tconstruct.gadgets.item.PiggyBackPackItem.CarryPotionEffect;
import slimeknights.tconstruct.gadgets.item.ShootProjectileDispenserBehavior;
import slimeknights.tconstruct.gadgets.item.ShurikenItem;
import slimeknights.tconstruct.shared.TinkerFood;
import slimeknights.tconstruct.world.block.FoliageType;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

/**
 * Contains any special tools unrelated to the base tools.
 * TODO: consider merging this into commons, the distinction of what is a gadget is getting pretty narrow.
 */
@SuppressWarnings("unused")
public final class TinkerGadgets extends TinkerModule {
  /* Block base properties */

  /*
   * Blocks
   */
  public static final ItemObject<PunjiBlock> punji = BLOCKS.register("punji", PunjiBlock::new, () -> builder(MapColor.PLANT, SoundType.GRASS).strength(3.0F).speedFactor(0.4F).noOcclusion().pushReaction(PushReaction.POPPED), TOOLTIP_BLOCK_ITEM);

  /*
   * Items
   */
  public static final ItemObject<PiggyBackPackItem> piggyBackpack = ITEMS.register("piggy_backpack", PiggyBackPackItem::new, props -> props.stacksTo(16));
  public static final EnumObject<FrameType,FancyItemFrameItem> itemFrame = ITEMS.registerEnum(FrameType.values(), "item_frame", (type, props) -> new FancyItemFrameItem(props, (world, pos, dir) -> new FancyItemFrameEntity(world, pos, dir, type)), ITEM_PROPS);
  public static final EnumObject<StandType,FancyArmorStandItem> armorStand = ITEMS.registerEnum(StandType.values(), "armor_stand", (type, props) -> new FancyArmorStandItem(props, type), props -> props.stacksTo(16));

  // throwballs
  @Deprecated
  public static final ItemObject<GlowBallItem> glowBall;
  @Deprecated
  public static final ItemObject<EFLNItem> efln;
  @Deprecated
  public static final ItemObject<ShurikenItem> quartzShuriken, flintShuriken;
  static {
    UnaryOperator<Item.Properties> THROWABLE_PROPS = props -> props.stacksTo(16);
    glowBall = ITEMS.register("glow_ball", GlowBallItem::new, THROWABLE_PROPS);
    efln = ITEMS.register("efln_ball", EFLNItem::new, THROWABLE_PROPS);
    quartzShuriken = ITEMS.register("quartz_shuriken", props -> new ShurikenItem(props, QuartzShurikenEntity::new), THROWABLE_PROPS);
    flintShuriken = ITEMS.register("flint_shuriken", props -> new ShurikenItem(props, FlintShurikenEntity::new), THROWABLE_PROPS);
  }

  // foods
  public static final EnumObject<FoliageType,FoodCakeBlock> cake;
  public static final ItemObject<FoodCakeBlock> magmaCake;
  static {
    // cakes always add a compost layer, and show the slice effects on the item as blocks lost their tooltip hook
    BiFunction<Block, Item.Properties, BlockItem> cakeItem = (block, props) -> new FoodCakeItem(block, props.stacksTo(1).compostable(ContextIntProviders.COMPOSTABLE_ALWAYS_ADD_ONE));
    cake = BLOCKS.registerEnum(FoliageType.values(), "cake", (type, props) -> {
      if (type == FoliageType.ICHOR) {
        return new InvertedCakeBlock(props, TinkerFood.ICHOR_CAKE, TinkerFood.ICHOR_CAKE_USE, EffectCombination.BLOCK);
      }
      return new FoodCakeBlock(props, TinkerFood.getCake(type), TinkerFood.getCakeUse(type), type == FoliageType.ENDER ? EffectCombination.ADD : EffectCombination.BLOCK);
    }, type -> cakeProps(), cakeItem);
    magmaCake = BLOCKS.register("magma_cake", props -> new FoodCakeBlock(props, TinkerFood.MAGMA_CAKE, TinkerFood.MAGMA_CAKE_USE, EffectCombination.BLOCK), TinkerGadgets::cakeProps, cakeItem);
  }

  // Shurikens

  /*
   * Entities
   */
  public static final DeferredHolder<? super EntityType<FancyItemFrameEntity>, EntityType<FancyItemFrameEntity>> itemFrameEntity = ENTITIES.register("fancy_item_frame", () ->
    EntityType.Builder.<FancyItemFrameEntity>of(
      FancyItemFrameEntity::new, MobCategory.MISC)
      .sized(0.5F, 0.5F)
      .setTrackingRange(10)
      .setUpdateInterval(Integer.MAX_VALUE)
      .setShouldReceiveVelocityUpdates(false)
  );
  public static final DeferredHolder<? super EntityType<FancyArmorStandEntity>, EntityType<FancyArmorStandEntity>> armorStandEntity = ENTITIES.register("armor_stand", () ->
    EntityType.Builder.<FancyArmorStandEntity>of(FancyArmorStandEntity::new, MobCategory.MISC).sized(0.5F, 1.975F).eyeHeight(1.7775F).clientTrackingRange(10));

  @Deprecated
  public static final DeferredHolder<? super EntityType<GlowballEntity>, EntityType<GlowballEntity>> glowBallEntity = ENTITIES.register("glow_ball", () ->
    EntityType.Builder.<GlowballEntity>of(GlowballEntity::new, MobCategory.MISC)
      .sized(0.25F, 0.25F)
      .setTrackingRange(4)
      .setUpdateInterval(10)
      .setShouldReceiveVelocityUpdates(true)
  );
  @Deprecated
  public static final DeferredHolder<? super EntityType<EFLNEntity>, EntityType<EFLNEntity>> eflnEntity = ENTITIES.register("efln_ball", () ->
    EntityType.Builder.<EFLNEntity>of(EFLNEntity::new, MobCategory.MISC)
      .sized(0.25F, 0.25F)
      .setTrackingRange(4)
      .setUpdateInterval(10)
      .setShouldReceiveVelocityUpdates(true));
  @Deprecated
  public static final DeferredHolder<? super EntityType<QuartzShurikenEntity>, EntityType<QuartzShurikenEntity>> quartzShurikenEntity = ENTITIES.register("quartz_shuriken", () ->
    EntityType.Builder.<QuartzShurikenEntity>of(QuartzShurikenEntity::new, MobCategory.MISC)
      .sized(0.25F, 0.25F)
      .setTrackingRange(4)
      .setUpdateInterval(10)
      .setShouldReceiveVelocityUpdates(true)
  );
  @Deprecated
  public static final DeferredHolder<? super EntityType<FlintShurikenEntity>, EntityType<FlintShurikenEntity>> flintShurikenEntity = ENTITIES.register("flint_shuriken", () ->
    EntityType.Builder.<FlintShurikenEntity>of(FlintShurikenEntity::new, MobCategory.MISC)
      .sized(0.25F, 0.25F)
      .setTrackingRange(4)
      .setUpdateInterval(10)
      .setShouldReceiveVelocityUpdates(true)
  );

  /*
   * Potions
   */
  public static final DeferredHolder<? super CarryPotionEffect, CarryPotionEffect> carryEffect = MOB_EFFECTS.register("carry", CarryPotionEffect::new);

  public TinkerGadgets() {}

  /**
   * Constructor wiring the mod bus, used to register the data attachments owned by this module.
   * TConstruct constructs gadgets via {@code new TinkerGadgets(modBus)} so the piggyback attachment register hooks the mod bus during construction (attachment DeferredRegisters cannot be registered from {@code commonSetup}).
   * @param modBus  Mod event bus
   */
  public TinkerGadgets(IEventBus modBus) {
    PiggybackCapability.register(modBus);
  }

  /** Properties for all cakes, must be a new instance each call */
  private static BlockBehaviour.Properties cakeProps() {
    return builder(SoundType.WOOL).forceSolidOn().strength(0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.POPPED);
  }

  /*
   * Events
   */
  @SubscribeEvent
  void commonSetup(final FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
      // cake composting is the compostable item component now, see the cake item above
      DispenserBlock.registerBehavior(glowBall, new ShootProjectileDispenserBehavior(glowBallEntity.get()));
      DispenserBlock.registerBehavior(efln, new ShootProjectileDispenserBehavior(eflnEntity.get()));
      DispenserBlock.registerBehavior(flintShuriken, new ShootProjectileDispenserBehavior(flintShurikenEntity.get()));
      DispenserBlock.registerBehavior(quartzShuriken, new ShootProjectileDispenserBehavior(quartzShurikenEntity.get()));
      armorStand.forEach((type, item) -> DispenserBlock.registerBehavior(item, new DispenseFancyArmorStand(type)));
    });
  }

  @SubscribeEvent
  void gatherRegistries(final GatherDataRegistryEntriesEvent event) {
    event.recipe(GadgetRecipeProvider::new);
  }

  @SubscribeEvent
  void entityAttributes(EntityAttributeCreationEvent event) {
    // 1.21 armor stands also set step height to 0, use the vanilla attributes so fancy stands match
    event.put(armorStandEntity.get(), ArmorStand.createAttributes().build());
  }

  /** Adds all relevant items to the creative tab, called by general tab */
  public static void addTabItems(ItemDisplayParameters itemDisplayParameters, Output output) {
    output.accept(punji);
    acceptStand(output, StandType.BAMBOO);
    acceptFrame(output, FrameType.CLEAR);
    acceptStand(output, StandType.CLEAR);
    acceptStand(output, StandType.BONE);
    acceptStand(output, StandType.NECROTIC_BONE);
    acceptFrame(output, FrameType.GOLD);
    acceptFrame(output, FrameType.REVERSED_GOLD);
    acceptFrame(output, FrameType.DIAMOND);
    acceptFrame(output, FrameType.MANYULLYN);
    acceptFrame(output, FrameType.NETHERITE);
    output.accept(piggyBackpack);
    accept(output, cake);
    output.accept(magmaCake);
  }

  /** Adds the given frame */
  private static void acceptFrame(Output output, FrameType frameType) {
    output.accept(itemFrame.get(frameType));
  }

  /** Adds the given armor stand */
  private static void acceptStand(Output output, StandType standType) {
    output.accept(armorStand.get(standType));
  }
}
