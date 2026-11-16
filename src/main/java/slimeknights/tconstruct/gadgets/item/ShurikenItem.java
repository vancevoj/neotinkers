package slimeknights.tconstruct.gadgets.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import slimeknights.mantle.util.TranslationHelper;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.gadgets.entity.shuriken.ShurikenEntityBase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiFunction;

/** @deprecated use {@link slimeknights.tconstruct.library.tools.item.ModifiableShurikenItem} */
@Deprecated
public class ShurikenItem extends SnowballItem {

  private final BiFunction<Level, Player, ShurikenEntityBase> entity;

  public ShurikenItem(Properties properties, BiFunction<Level, Player, ShurikenEntityBase> entity) {
    super(properties);
    this.entity = entity;
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    level.playSound(null, player.getX(), player.getY(), player.getZ(), Sounds.SHURIKEN_THROW.getSound(), SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
    player.getCooldowns().addCooldown(stack, 4);
    if(!level.isClientSide()) {
      ShurikenEntityBase entity = this.entity.apply(level, player);
      entity.setItem(stack);
      entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
      level.addFreshEntity(entity);
    }
    player.awardStat(Stats.ITEM_USED.get(this));
    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }

    return InteractionResult.SUCCESS;
  }

  @SuppressWarnings("deprecation")
  @Override
  public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    List<Component> lines = new ArrayList<>();
    TranslationHelper.addOptionalTooltip(stack, lines);
    lines.forEach(tooltip);
    super.appendHoverText(stack, context, display, tooltip, flag);
  }
}
