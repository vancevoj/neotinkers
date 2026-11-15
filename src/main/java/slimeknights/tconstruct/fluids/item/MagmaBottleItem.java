package slimeknights.tconstruct.fluids.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import slimeknights.mantle.item.ConstantFluidContainerWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;

import java.util.function.Consumer;

/** Magma bottle instance, which lights the drinker on fire */
public class MagmaBottleItem extends Item {
  private final int fireTime;
  public MagmaBottleItem(Properties props, int fireTime) {
    // drinking (use, animation, 32 tick duration, sounds) comes from the consumable component since 1.21.2
    super(props.component(DataComponents.CONSUMABLE, Consumables.defaultDrink().build()));
    this.fireTime = fireTime;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
    super.appendHoverText(stack, context, display, tooltip, flagIn);
    tooltip.accept(Component.translatable(
      "potion.withDuration",
      Blocks.FIRE.getName(),
      StringUtil.formatTickDuration(fireTime * 20, 20.0f)
    ).withStyle(MobEffectCategory.HARMFUL.getTooltipFormatting()));
  }

  @Override
  public int getUseDuration(ItemStack pStack, LivingEntity living) {
    return 32;
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    living.igniteForSeconds(fireTime);
    ItemStackTemplate remainder = stack.getCraftingRemainder();
    // consumable handles the sound, stats, and shrinking the stack (not in creative)
    ItemStack result = super.finishUsingItem(stack, level, living);
    Player player = living instanceof Player p ? p : null;
    if (remainder != null && (player == null || !player.getAbilities().instabuild)) {
      ItemStack container = remainder.create();
      if (result.isEmpty()) {
        return container;
      }
      if (player != null) {
        // adds to the inventory or drops serverside, like vanilla use remainders
        player.handleExtraItemsCreatedOnUse(container);
      }
    }
    return result;
  }

  /** Builds the fluid handler for the given item access, for registration in {@code RegisterCapabilitiesEvent}. */
  public ResourceHandler<FluidResource> getFluidHandler(ItemAccess access) {
    return new ConstantFluidContainerWrapper(new FluidStack(TinkerFluids.magma.get(), FluidValues.BOTTLE), access);
  }
}
