package slimeknights.tconstruct.library.tools.item.ranged;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbility;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.item.ModifiableItemClientExtension;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.EnchantmentModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.DurabilityDisplayModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.EntityInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.definition.module.mining.MiningSpeedToolHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.helper.ToolHarvestLogic;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.item.IClientExtensionItem;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.MaterialItem;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook.KEY_DRAWTIME;

/** Base class for any items that launch projectiles */
public abstract class ModifiableLauncherItem extends ProjectileWeaponItem implements IModifiableDisplay, IClientExtensionItem {
  /** Persistent data key for the ammo being used on drawing back the bow. */
  public static final Identifier KEY_DRAWBACK_AMMO = TConstruct.getResource("drawback_ammo");

  /** Tool definition for the given tool */
  @Getter
  private final ToolDefinition toolDefinition;

  /** Cached tool for rendering on UIs */
  private ItemStack toolForRendering;

  public ModifiableLauncherItem(Properties properties, ToolDefinition toolDefinition) {
    super(TooltipUtil.hideVanillaEnchantments(properties));
    this.toolDefinition = toolDefinition;
  }


  /* Basic properties */

  @Override
  public int getMaxStackSize(ItemStack stack) {
    return 1;
  }

  @Override
  public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
    return true;
  }


  /* Enchanting */
  // 1.21.2 removed Item#isEnchantable and getEnchantmentValue: without the ENCHANTABLE component the tool cannot be enchanted in the enchanting table

  /** Replaces the removed {@code isBookEnchantable}, NeoForge now asks this method for enchanted books in the anvil */
  @Override
  public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
    return false;
  }

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
  public Entity createEntity(Level world, Entity original, ItemStack stack) {
    return IndestructibleItemEntity.createFrom(world, original, stack);
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
  // 1.21.2 removed isRepairable and isValidRepairItem: without the REPAIRABLE component the anvil cannot repair the tool, repair is handled in the tinker station

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
    // emulate ItemStack.hurtAndBreak; we always return 0 to skip vanilla's handling so tools use our damage logic
    // since 1.21.2 hurtAndBreak calls this before checking isDamageableItem, so check it here to keep the old behavior
    // checks the tool rather than isDamageableItem, matching ModifiableItem; returning 0 means vanilla never damages the components itself
    if (ToolDamageUtil.isDamageable(stack) && ToolDamageUtil.damage(ToolStack.from(stack), amount, damager, stack)) {
      // the tool is not consumed when broken, so pass the stack itself (vanilla passes a copy of the stack it just shrank)
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


  /* Modifier interactions */

  @Override
  public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
    // vanilla reads blocking and shield disabling from components since 1.21.5, so keep them in sync with the tool actions
    // runs for mobs too, so mobs holding tools can block and disable shields like in 1.21.1
    ModifierUtil.updateCombatComponents(stack, ToolStack.from(stack), level.registryAccess());
    // 1.21.1 only ticked player inventories, since 1.21.5 the equipment of every living entity ticks; keep modifier hooks player only
    if (entity instanceof Player) {
      InventoryTickModifierHook.heldInventoryTick(stack, level, entity, slot);
    }
  }

  /** Since 1.21.5 vanilla only ticks items server side, modifier hooks ran on both sides before so tick them on the client too */
  @Override
  public void clientInventoryTick(ItemStack stack, Level level, Entity entity, @Nullable EquipmentSlot slot) {
    if (entity instanceof Player) {
      InventoryTickModifierHook.heldInventoryTick(stack, level, entity, slot);
    }
  }

  @Override
  public boolean overrideStackedOnOther(ItemStack held, Slot slot, ClickAction action, Player player) {
    return SlotStackModifierHook.overrideStackedOnOther(held, slot, action, player);
  }

  @Override
  public boolean overrideOtherStackedOnMe(ItemStack slotStack, ItemStack held, Slot slot, ClickAction action, Player player, SlotAccess access) {
    return SlotStackModifierHook.overrideOtherStackedOnMe(slotStack, held, slot, action, player, access);
  }


  /* Attacking */

  @Override
  public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
    return EntityInteractionModifierHook.leftClickEntity(stack, player, target);
  }

  @Override
  public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
    // since 26.1 the stack may be an item stack template, which has no tool data
    return stack instanceof ItemStack itemStack && ModifierUtil.canPerformAction(ToolStack.from(itemStack), itemAbility);
  }

  @Override
  public Multimap<Attribute,AttributeModifier> getAttributeModifiers(IToolStackView tool, EquipmentSlot slot) {
    return AttributesModifierHook.getHeldAttributeModifiers(tool, slot);
  }

  @Override
  public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
    return buildAttributeModifiers(stack, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
  }

  // NeoForge 26.3 removed canDisableShield; shield disabling reads the WEAPON component, see ModifierUtil#updateCombatComponents

  /**
   * Vanilla's mob-firing entry point. Tinkers' launchers fire entirely through {@link #use}/{@link #releaseUsing}, so
   * this is never invoked; implemented as a no-op to satisfy the abstract {@link ProjectileWeaponItem} contract.
   */
  @Override
  protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {}


  /* Arrow logic */

  @Override
  public int getUseDuration(ItemStack stack, LivingEntity entity) {
    return 72000;
  }

  @Override
  public abstract ItemUseAnimation getUseAnimation(ItemStack pStack);

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level pLevel, LivingEntity living) {
    ToolStack tool = ToolStack.from(stack);
    int duration = getUseDuration(stack, living);
    for (ModifierEntry entry : tool.getModifiers()) {
      entry.getHook(ModifierHooks.TOOL_USING).beforeReleaseUsing(tool, entry, living, duration, 0, ModifierEntry.EMPTY);
    }
    return stack;
  }

  @Override
  public void onStopUsing(ItemStack stack, LivingEntity entity, int timeLeft) {
    onStopUsing(ToolStack.from(stack), entity, timeLeft);
  }

  /** Same as {@link #onStopUsing(ItemStack, LivingEntity, int)} but uses a tool. */
  protected void onStopUsing(IToolStackView tool, LivingEntity entity, int timeLeft) {
    UsingToolModifierHook.afterStopUsing(tool, entity, timeLeft);
    ModDataNBT data = tool.getPersistentData();
    data.remove(KEY_DRAWTIME);
    data.remove(KEY_DRAWBACK_AMMO);
  }

  @Override
  public void onUseTick(Level level, LivingEntity living, ItemStack bow, int chargeRemaining) {
    // play the sound at the end of loading as an indicator its loaded, texture is another indicator
    int duration = getUseDuration(bow, living);
    if (!level.isClientSide()) {
      if (duration - chargeRemaining == ModifierUtil.getPersistentInt(bow, KEY_DRAWTIME, -1)) {
        level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 0.75F, 1.0F);
      }
    }
    ToolStack tool = ToolStack.from(bow);
    for (ModifierEntry entry : tool.getModifiers()) {
      entry.getHook(ModifierHooks.TOOL_USING).onUsingTick(tool, entry, living, duration, chargeRemaining, ModifierEntry.EMPTY);
    }
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
  // getDefaultTooltipHideFlags is gone: vanilla enchantments are hidden by the TOOLTIP_DISPLAY component (TooltipUtil#hideVanillaEnchantments),
  // attributes are hidden when holding shift or control through TooltipUtil#hidesAttributeTooltip


  /* Display */

  @Override
  public ItemStack getRenderTool() {
    if (toolForRendering == null) {
      toolForRendering = ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition());
    }
    return toolForRendering;
  }

  /** Registered from the client extensions event, NeoForge 26.3 no longer calls this automatically */
  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(ModifiableItemClientExtension.INSTANCE);
  }


  /* Misc */

  @Override
  public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
    return shouldCauseReequipAnimation(oldStack, newStack, false);
  }

  @Override
  public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
    return ModifiableItem.shouldCauseReequip(oldStack, newStack, slotChanged);
  }


  /* Harvest logic, mostly used by modifiers but technically would let you make a pickaxe bow */

  @Override
  public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
    return IsEffectiveToolHook.isEffective(ToolStack.from(stack), state);
  }

  @Override
  public boolean mineBlock(ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
    return ToolHarvestLogic.mineBlock(stack, worldIn, state, pos, entityLiving);
  }

  @Override
  public float getDestroySpeed(ItemStack stack, BlockState state) {
    return MiningSpeedToolHook.getDestroySpeed(stack, state);
  }
  // NeoForge 1.21 removed Item#onBlockStartBreak; AoE/expanded block breaking (ToolHarvestLogic.handleBlockBreak)
  // is now triggered via the game-bus BreakBlockEvent handler in ToolEvents#onBlockBreak.


  /* Multishot helper */

  /** Gets the angle to fire the first arrow, each additional arrow offsets an additional 10 degrees */
  public static float getAngleStart(int count) {
    return -5 * (count - 1);
  }
}
