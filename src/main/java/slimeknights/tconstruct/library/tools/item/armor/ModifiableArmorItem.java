package slimeknights.tconstruct.library.tools.item.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForgeMod;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IClientExtensionItem;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.RenderToolCache;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.Util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Modifiable armor item. Since 1.21.5 there is no {@code ArmorItem}, wearing an item is driven by the {@link DataComponents#EQUIPPABLE} component,
 * which the constructor adds for the armor slot. All armor stats remain dynamic from the tool through {@link #getAttributeModifiers(IToolStackView, EquipmentSlot)}.
 */
public class ModifiableArmorItem extends Item implements IModifiableDisplay, IClientExtensionItem {
  /** Volatile modifier tag to make piglins neutal when worn */
  public static final Identifier PIGLIN_NEUTRAL = TConstruct.getResource("piglin_neutral");
  /** Volatile modifier tag to make this item an elytra */
  public static final Identifier ELYTRA = TConstruct.getResource("elyta");
  /** Volatile flag for a boot item to walk on powdered snow. Cold immunity is handled through a tag */
  public static final Identifier SNOW_BOOTS = TConstruct.getResource("snow_boots");
  /** Volatile flag for an item to act as an enderman mask, stopping them from getting angry. */
  public static final Identifier ENDERMASK = TConstruct.getResource("endermask");

  @Getter
  private final ToolDefinition toolDefinition;
  /** Armor type for this item, replaces the type from the removed {@code ArmorItem} */
  @Getter
  protected final ArmorType type;
  /** Cache of the tool built for rendering */
  private final RenderToolCache toolForRendering = new RenderToolCache();
  /**
   * Creates a new armor item
   * @param materialIn      Armor material, provides the equip sound
   * @param type            Armor type
   * @param builderIn       Item properties
   * @param toolDefinition  Tool definition for the item
   * @param assetName       Name of the equipment asset, should match the armor model name used for rendering
   */
  public ModifiableArmorItem(DummyArmorMaterial materialIn, ArmorType type, Properties builderIn, ToolDefinition toolDefinition, Identifier assetName) {
    super(makeProperties(materialIn, type, builderIn, assetName));
    this.type = type;
    this.toolDefinition = toolDefinition;
  }

  public ModifiableArmorItem(DummyArmorMaterial materialIn, ArmorType type, Properties builderIn, ToolDefinition toolDefinition) {
    this(materialIn, type, builderIn, toolDefinition, materialIn.getId());
  }

  public ModifiableArmorItem(ModifiableArmorMaterial material, ArmorType type, Properties properties, Identifier assetName) {
    this(material, type, properties, Objects.requireNonNull(material.getArmorDefinition(type), "Missing tool definition for " + type.getName()), assetName);
  }

  public ModifiableArmorItem(ModifiableArmorMaterial material, ArmorType type, Properties properties) {
    this(material, type, properties, material.getId());
  }

  /**
   * Adds the components this armor needs to the item properties.
   * The equippable component replaces {@code ArmorItem}: it lets the item be worn in its slot (right click swap, dispensers, mob equipment),
   * damages it when the wearer is hurt, and plays the material equip sound.
   * It needs an equipment asset ID, otherwise vanilla leaves the stack out of the humanoid render state (Tinkers' armor layer never sees it and helmets render as head items).
   * The asset needs no JSON: the armor model dispatcher draws the armor and tells the vanilla layers to skip it.
   * @param material    Armor material, provides the equip sound
   * @param type        Armor type, provides the slot
   * @param properties  Properties to update
   * @param assetName   Name of the equipment asset
   * @return  Updated properties
   */
  protected static Properties makeProperties(DummyArmorMaterial material, ArmorType type, Properties properties, Identifier assetName) {
    // items are constructed after sounds are registered, so this gives a registry holder that syncs by ID
    Holder<SoundEvent> equipSound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(material.getEquipSound());
    Equippable equippable = Equippable.builder(type.getSlot()).setEquipSound(equipSound).setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, assetName)).build();
    return TooltipUtil.hideVanillaEnchantments(properties).component(DataComponents.EQUIPPABLE, equippable);
  }

  /** Gets the equipment slot for this armor, replaces the method from the removed {@code ArmorItem} */
  public EquipmentSlot getEquipmentSlot() {
    return type.getSlot();
  }

  /* Basic properties */

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return 1;
  }

  @Override
  public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
    return ModifierUtil.checkVolatileFlag(stack, PIGLIN_NEUTRAL);
  }

  @Override
  public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
    return type == ArmorType.BOOTS && ModifierUtil.checkVolatileFlag(stack, SNOW_BOOTS);
  }

  /** Replaces the NeoForge {@code isEnderMask} hook, since 1.21.4 it also hides the gaze from creakings */
  @Override
  public boolean isGazeDisguise(ItemStack stack, Player player, @Nullable LivingEntity entity) {
    return type == ArmorType.HELMET && ModifierUtil.checkVolatileFlag(stack, ENDERMASK);
  }

  @Override
  public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
    // since 26.1 the stack may be an item stack template, which has no tool data
    return stack instanceof ItemStack itemStack && ModifierUtil.canPerformAction(ToolStack.from(itemStack), itemAbility);
  }

  @Override
  public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
    return true;
  }


  /* Enchantments */
  // 1.21.2 removed Item#isEnchantable: without the ENCHANTABLE component the armor cannot be enchanted in the enchanting table

  // the removed isBookEnchantable is replaced by ToolEvents#onAnvilUpdate rejecting enchanted books. supportsEnchantment keeps its default
  // as it also gates /enchant and enchanting loot functions, which accepted tools in 1.21.1

  /** Reports the tool's virtual modifier enchantments to gameplay logic, they are never stored in NBT */
  @Override
  public int getEnchantmentLevel(ItemInstance stack, Holder<Enchantment> enchantment) {
    return EnchantmentModifierHook.getEnchantmentLevel(stack, enchantment);
  }

  /** Reports the tool's virtual modifier enchantments to gameplay logic, they are never stored in NBT */
  @Override
  public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
    return EnchantmentModifierHook.getEnchantmentComponent(stack);
  }


  /* Loading */

  @Override
  public void onCraftedBy(ItemStack stack, Player playerIn) {
    ToolStack.ensureInitialized(stack, getToolDefinition());
  }

  @Override
  public InteractionResult use(Level levelIn, Player playerIn, InteractionHand handIn) {
    if (playerIn.isCrouching()) {
      ItemStack stack = playerIn.getItemInHand(handIn);
      InteractionResult result = ToolInventoryCapability.tryOpenContainer(stack, null, getToolDefinition(), playerIn, Util.getSlotType(handIn));
      if (result.consumesAction()) {
        return result;
      }
    }
    // swaps with the armor slot through the equippable component, as ArmorItem#use did
    return super.use(levelIn, playerIn, handIn);
  }


  /* Display */

  @Override
  public boolean isFoil(ItemStack stack) {
    // we use enchantments to handle some modifiers, so don't glow from them
    // however, if a modifier wants to glow let them
    return ModifierUtil.checkVolatileFlag(stack, SHINY);
  }


  /* Item entity */

  @Override
  public boolean hasCustomEntity(ItemStack stack) {
    return IndestructibleItemEntity.hasCustomEntity(stack);
  }

  @Nullable
  @Override
  public Entity createEntity(Level level, Entity original, ItemStack stack) {
    return IndestructibleItemEntity.createFrom(level, original, stack);
  }

  @Override
  public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
    return IndestructibleItemEntity.canBeHurtBy(stack, source);
  }

  @Override
  public void onDestroyed(ItemEntity entity, DamageSource damageSource) {
    ToolInventoryCapability.onDestroyed(entity);
  }


  /* Damage/Durability */
  // 1.21.2 removed isRepairable and isValidRepairItem: without the REPAIRABLE component the anvil cannot repair the armor, repair is handled in the tinker station

  /** Replaces the removed {@code isRepairable} for crafting grid and grindstone combining */
  @Override
  public boolean isCombineRepairable(ItemStack stack) {
    return false;
  }

  @Override
  public int getMaxDamage(ItemStack stack) {
    return ToolDamageUtil.getFakeMaxDamage(stack);
  }

  @Override
  public int getDamage(ItemStack stack) {
    return ToolStack.from(stack).getDamage();
  }

  @Override
  public void setDamage(ItemStack stack, int damage) {
    ToolStack.from(stack).setDamage(damage);
  }

  @Override
  public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T damager, Consumer<ItemStack> onBroken) {
    // We basically emulate Itemstack.damageItem here. We always return 0 to skip the handling in ItemStack.
    // If we don't tools ignore our damage logic
    // since 1.21.2 hurtAndBreak calls this before checking isDamageableItem (it used to skip us), so check it here to keep the old behavior
    // checks the tool rather than isDamageableItem, matching ModifiableItem; returning 0 means vanilla never damages the components itself
    if (ToolDamageUtil.isDamageable(stack) && ToolDamageUtil.damage(ToolStack.from(stack), amount, damager, stack)) {
      // the armor is not consumed when broken, so pass the stack itself (vanilla passes a copy of the stack it just shrank)
      onBroken.accept(stack);
    }

    return 0;
  }


  /* Durability display */

  @Override
  public boolean isBarVisible(ItemStack pStack) {
    return DurabilityDisplayModifierHook.showDurabilityBar(pStack);
  }

  @Override
  public int getBarColor(ItemStack pStack) {
    return DurabilityDisplayModifierHook.getDurabilityRGB(pStack);
  }

  @Override
  public int getBarWidth(ItemStack pStack) {
    return DurabilityDisplayModifierHook.getDurabilityWidth(pStack);
  }


  /* Armor properties */

  /** Base resource location for the armor stat attribute modifier IDs, suffixed with the slot and stat */
  private static final Identifier ARMOR_MODIFIER = TConstruct.getResource("armor");

  @Override
  public Multimap<Attribute,AttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
    if (slot != getEquipmentSlot()) {
      return ImmutableMultimap.of();
    }

    ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
    if (!tool.isBroken()) {
      // base stats; in 1.21 attribute modifiers are keyed by a stable Identifier (was UUID+name) and use ADD_VALUE
      StatsNBT statsNBT = tool.getStats();
      String slotName = slot.getName();
      float armor = statsNBT.get(ToolStats.ARMOR);
      if (armor > 0) {
        builder.put(Attributes.ARMOR.value(), new AttributeModifier(ARMOR_MODIFIER.withSuffix("." + slotName + ".armor"), armor, AttributeModifier.Operation.ADD_VALUE));
      }
      float toughness = statsNBT.get(ToolStats.ARMOR_TOUGHNESS);
      if (toughness > 0) {
        builder.put(Attributes.ARMOR_TOUGHNESS.value(), new AttributeModifier(ARMOR_MODIFIER.withSuffix("." + slotName + ".toughness"), toughness, AttributeModifier.Operation.ADD_VALUE));
      }
      double knockbackResistance = statsNBT.get(ToolStats.KNOCKBACK_RESISTANCE);
      if (knockbackResistance > 0) {
        builder.put(Attributes.KNOCKBACK_RESISTANCE.value(), new AttributeModifier(ARMOR_MODIFIER.withSuffix("." + slotName + ".knockback_resistance"), knockbackResistance, AttributeModifier.Operation.ADD_VALUE));
      }
      // grab attributes from modifiers
      BiConsumer<Attribute,AttributeModifier> attributeConsumer = builder::put;
      for (ModifierEntry entry : tool.getModifierList()) {
        entry.getHook(ModifierHooks.ATTRIBUTES).addAttributes(tool, entry, slot, attributeConsumer);
      }
    }

    return builder.build();
  }

  /** Attribute modifier ID for elytra flight on the chestplate */
  private static final Identifier ELYTRA_FLIGHT = TConstruct.getResource("elytra_flight");
  /** Attribute modifier enabling elytra flight */
  private static final AttributeModifier ELYTRA_FLIGHT_MODIFIER = new AttributeModifier(ELYTRA_FLIGHT, 1, AttributeModifier.Operation.ADD_VALUE);

  @Override
  public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
    ItemAttributeModifiers modifiers = buildAttributeModifiers(stack, getEquipmentSlot());
    // NeoForge 26.3 removed canElytraFly; gliding is enabled by the gliding flight attribute instead, hidden as the elytra modifier already shows in the tooltip
    if (canElytraFly(stack)) {
      ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
      for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
        builder.add(entry.attribute(), entry.modifier(), entry.slot(), entry.display());
      }
      builder.add(NeoForgeMod.GLIDING_FLIGHT, ELYTRA_FLIGHT_MODIFIER, EquipmentSlotGroup.CHEST, ItemAttributeModifiers.Display.hidden());
      modifiers = builder.build();
    }
    return modifiers;
  }


  /* Elytra */

  /**
   * Checks if the given stack can fly as an elytra. Replaces the NeoForge {@code canElytraFly} hook, used to add the gliding flight attribute.
   * @param stack  Stack to check
   * @return  True if the stack works as an elytra
   */
  public boolean canElytraFly(ItemStack stack) {
    return type == ArmorType.CHESTPLATE && !ToolDamageUtil.isBroken(stack) && ModifierUtil.checkVolatileFlag(stack, ELYTRA);
  }

  /**
   * Runs the elytra flight modifier hooks, replacement for the removed NeoForge {@code elytraFlightTick} hook.
   * Called from the inventory tick while the entity is gliding with this chestplate. Durability is lost in {@link #onGlideDamage(ItemStack, LivingEntity, EquipmentSlot)}.
   * @param stack        Chestplate stack
   * @param entity       Entity gliding
   * @param flightTicks  Number of ticks the entity has been flying
   * @return  True to keep flying, false to stop
   */
  public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
    if (type == ArmorType.CHESTPLATE) {
      ToolStack tool = ToolStack.from(stack);
      if (!tool.isBroken() && tool.getVolatileData().getBoolean(ELYTRA)) {
        // if any modifier says stop flying, stop flying
        for (ModifierEntry entry : tool.getModifierList()) {
          if (entry.getHook(ModifierHooks.ELYTRA_FLIGHT).elytraFlightTick(tool, entry, entity, flightTicks)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /** Damages the chestplate while gliding, vanilla calls this every 20 flight ticks which matches the old elytra flight tick damage */
  @Override
  public void onGlideDamage(ItemStack stack, LivingEntity wearer, EquipmentSlot slot) {
    ToolDamageUtil.damageAnimated(ToolStack.from(stack), 1, wearer, slot);
  }


  /* Ticking */

  @Override
  public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
    armorInventoryTick(stack, level, entity, slot);
  }

  /** Since 1.21.5 vanilla only ticks items server side, modifier hooks ran on both sides before so tick them on the client too */
  @Override
  public void clientInventoryTick(ItemStack stack, Level level, Entity entity, @Nullable EquipmentSlot slot) {
    armorInventoryTick(stack, level, entity, slot);
  }

  /**
   * Shared inventory tick logic for both sides.
   * @param stack   Armor stack
   * @param level   Level containing the entity
   * @param entity  Entity holding the armor
   * @param slot    Equipment slot from the vanilla tick, null for main inventory slots that are not selected
   */
  protected void armorInventoryTick(ItemStack stack, Level level, Entity entity, @Nullable EquipmentSlot slot) {
    // don't care about non-living, they skip most tool context
    if (entity instanceof LivingEntity living) {
      EquipmentSlot armorSlot = getEquipmentSlot();
      boolean isCorrectSlot = living.getItemBySlot(armorSlot) == stack;
      // 1.21.1 only ticked player inventories, since 1.21.5 the equipment of every living entity ticks; keep modifier hooks player only
      if (living instanceof Player) {
        ToolStack tool = ToolStack.from(stack);
        if (!level.isClientSide()) {
          tool.ensureHasData();
        }
        List<ModifierEntry> modifiers = tool.getModifierList();
        if (!modifiers.isEmpty()) {
          // vanilla no longer passes the inventory index, so use the equipment slot index (or not found for the main inventory)
          int itemSlot = InventoryTickModifierHook.getSlotIndex(entity, slot);
          boolean isSelected = slot == EquipmentSlot.MAINHAND;
          // we pass in the stack for most custom context, but for the sake of armor its easier to tell them that this is the correct slot for effects
          for (ModifierEntry entry : modifiers) {
            entry.getHook(ModifierHooks.INVENTORY_TICK).onInventoryTick(tool, entry, level, living, itemSlot, isSelected, isCorrectSlot, stack);
          }
        }
      }

      // NeoForge 26.3 removed the elytra flight tick item hook, so run the flight hooks while gliding with this chestplate (any entity, like the old flight update)
      // equipment ticks before travel in LivingEntity#aiStep, so this runs each gliding tick before vanilla updates the flight
      if (isCorrectSlot && slot == armorSlot && living.isFallFlying() && !living.onGround() && !living.isPassenger() && !living.hasEffect(MobEffects.LEVITATION)
          && !elytraFlightTick(stack, living, living.getFallFlyingTicks()) && !level.isClientSide()) {
        // matches the old behavior, where the server cleared the flying flag when the hook said to stop
        living.stopFallFlying();
      }
    }
  }

  @Override
  public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
    return SlotStackModifierHook.overrideStackedOnOther(held, slot, action, player) || super.overrideStackedOnOther(held, slot, action, player);
  }

  @Override
  public boolean overrideOtherStackedOnMe(ItemStack slotStack, ItemStack held, Slot slot, ClickAction action, Player player, SlotAccess access) {
    return SlotStackModifierHook.overrideOtherStackedOnMe(slotStack, held, slot, action, player, access) || super.overrideOtherStackedOnMe(slotStack, held, slot, action, player, access);
  }


  /* Tooltips */

  @Override
  public Component getName(ItemStack stack) {
    // 1.21 removed Item#getRarity(ItemStack), so style the name with the tool rarity directly
    return MaterialItem.withRarity(ToolNameHook.getName(getToolDefinition(), stack), RarityModule.getRarity(stack));
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new ArrayList<>();
    TooltipUtil.addInformation(this, stack, context.level(), lines, SafeClientAccess.getTooltipKey(), flag);
    lines.forEach(tooltip);
  }

  @Override
  public List<Component> getStatInformation(IToolStackView tool, @Nullable Player player, List<Component> tooltips, TooltipKey key, TooltipFlag tooltipFlag) {
    tooltips = TooltipUtil.getArmorStats(tool, player, tooltips, key, tooltipFlag);
    TooltipUtil.addAttributes(this, tool, player, tooltips, TooltipUtil.SHOW_ARMOR_ATTRIBUTES, getEquipmentSlot());
    return tooltips;
  }
  // getDefaultTooltipHideFlags is gone: vanilla enchantments are hidden by the TOOLTIP_DISPLAY component (TooltipUtil#hideVanillaEnchantments),
  // attributes are hidden when holding shift or control through TooltipUtil#hidesAttributeTooltip

  /* Display items */

  @Override
  public ItemStack getRenderTool() {
    return toolForRendering.get(() -> ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition()));
  }
}
