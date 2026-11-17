package slimeknights.tconstruct.library.tools.item;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.SlotStackModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.build.RarityModule;
import slimeknights.tconstruct.library.tools.IndestructibleItemEntity;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.definition.module.display.ToolNameHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.part.MaterialItem;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.entity.ModifiableArrow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Modifiable item that is usable as arrows in a bow */
public class ModifiableArrowItem extends ArrowItem implements IModifiableDisplay {
  /** Tool definition for the given tool */
  @Getter
  private final ToolDefinition toolDefinition;
  /** Cached tool for rendering on UIs */
  private final RenderToolCache toolForRendering = new RenderToolCache();

  public ModifiableArrowItem(Properties props, ToolDefinition toolDefinition) {
    super(TooltipUtil.hideVanillaEnchantments(props));
    this.toolDefinition = toolDefinition;
  }


  /* Arrowing */

  @Override
  public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, @Nullable ItemStack weapon) {
    ModifiableArrow arrow = new ModifiableArrow(level, shooter, weapon);
    arrow.onCreate(stack, shooter);
    return arrow;
  }

  @Override
  public boolean isInfinite(ItemStack stack, ItemStack bow, LivingEntity shooter) {
    return false;
  }


  /* Shurikening */

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    // only throw arrows if they have the throwable tool action. Useful for the other style of projectile in addons, or a really weird arrow modifier.
    if (stack.is(TinkerTags.Items.THROWN_AMMO)) {
      level.playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.SHURIKEN_THROW.getSound(), SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
      player.getCooldowns().addCooldown(stack, 10);
      if (!level.isClientSide()) {
        ModifiableArrow arrow = new ModifiableArrow(level, player);
        IToolStackView tool = arrow.onCreate(stack, player);
        float velocity = ConditionalStatModifierHook.getModifiedStat(tool, player, ToolStats.VELOCITY);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 1.0F);
        level.addFreshEntity(arrow);
      }

      player.awardStat(Stats.ITEM_USED.get(this));
      if (!player.getAbilities().instabuild) {
        stack.shrink(1);
      }

      return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
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


  /* Indestructible items */

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


  /* Modifier interactions */

  @Override
  public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
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
  // getDefaultTooltipHideFlags is gone: vanilla enchantments are hidden by the TOOLTIP_DISPLAY component (TooltipUtil#hideVanillaEnchantments)

  @Override
  public List<Component> getStatInformation(IToolStackView tool, @Nullable Player player, List<Component> tooltips, TooltipKey key, TooltipFlag tooltipFlag) {
    return TooltipUtil.getAmmoStats(tool, player, tooltips, key, tooltipFlag);
  }

  /* Display items */

  @Override
  public ItemStack getRenderTool() {
    return toolForRendering.get(() -> ToolBuildHandler.buildToolForRendering(this, this.getToolDefinition()));
  }
}
