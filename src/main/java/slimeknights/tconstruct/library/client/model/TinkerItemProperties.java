package slimeknights.tconstruct.library.client.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.ToolDataComponents;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/**
 * Properties for tinker tools. In 26.3 properties are global and selected by the client item JSON, so they are registered once by ID instead of per item.
 * Values match the old item property functions so the converted override thresholds keep working.
 */
public class TinkerItemProperties {
  /** ID for broken property */
  private static final Identifier BROKEN_ID = TConstruct.getResource("broken");
  /** ID for ammo property */
  private static final Identifier AMMO_ID = TConstruct.getResource("ammo");
  /** ID for the pulling property */
  private static final Identifier CHARGING_ID = TConstruct.getResource("charging");
  /** ID for the pull property */
  private static final Identifier CHARGE_ID = TConstruct.getResource("charge");
  /** ID for the cast fishing rods */
  private static final Identifier CAST_ID = TConstruct.getResource("cast");
  /** ID of fireworks rocket for NBT check */
  private static final String FIREWORKS_ID = "minecraft:firework_rocket";

  /** Gets the holder from the item owner */
  @Nullable
  private static LivingEntity getHolder(@Nullable ItemOwner owner) {
    return owner == null ? null : owner.asLivingEntity();
  }

  /** Property declaring broken */
  public record Broken() implements ConditionalItemModelProperty {
    public static final Broken INSTANCE = new Broken();
    public static final MapCodec<Broken> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
      return ToolDamageUtil.isBroken(stack);
    }

    @Override
    public MapCodec<Broken> type() {
      return MAP_CODEC;
    }
  }

  /** Int declaring ammo type, 1 for arrows, 2 for fireworks */
  public record Ammo() implements RangeSelectItemModelProperty {
    public static final Ammo INSTANCE = new Ammo();
    public static final MapCodec<Ammo> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // tools store their NBT in the tconstruct:tool_data component, NOT minecraft:custom_data; reading the latter
      // always came back empty so the loaded crossbow model never rendered (issue #13). Mirror ModifierUtil.
      CompoundTag nbt = ToolDataComponents.getTag(stack);
      if (nbt != null && nbt.get(ToolStack.TAG_PERSISTENT_MOD_DATA) instanceof CompoundTag persistentData) {
        CompoundTag ammo = persistentData.getCompoundOrEmpty(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO.toString());
        if (!ammo.isEmpty()) {
          // no sense having two keys for ammo, just set 1 for arrow, 2 for fireworks
          return FIREWORKS_ID.equals(ammo.getStringOr("id", "")) ? 2 : 1;
        }
      }
      return 0;
    }

    @Override
    public MapCodec<Ammo> type() {
      return MAP_CODEC;
    }
  }

  /** Number indicating the tool is charging or blocking */
  public record Charging() implements RangeSelectItemModelProperty {
    public static final Charging INSTANCE = new Charging();
    public static final MapCodec<Charging> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      LivingEntity holder = getHolder(owner);
      if (holder != null && holder.isUsingItem() && holder.getUseItem() == stack) {
        ItemUseAnimation anim = stack.getUseAnimation();
        if (anim == ItemUseAnimation.BLOCK) {
          return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 2.5f : 2;
        }
        // TODO 1.21: space this out a bit more
        if (anim == ItemUseAnimation.TRIDENT) {
          // shouldn't need to worry about arrows on spearing, everything supporting arrows uses just bow or block
          return 1.75f;
        }
        if (anim != ItemUseAnimation.EAT && anim != ItemUseAnimation.DRINK) {
          return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 1.5f : 1;
        }
      }
      return 0;
    }

    @Override
    public MapCodec<Charging> type() {
      return MAP_CODEC;
    }
  }

  /** Property for bow pull amount */
  public record Charge() implements RangeSelectItemModelProperty {
    public static final Charge INSTANCE = new Charge();
    public static final MapCodec<Charge> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      LivingEntity holder = getHolder(owner);
      if (holder == null || holder.getUseItem() != stack) {
        return 0.0F;
      }
      int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
      return drawtime == -1 ? 0 : (float)(stack.getUseDuration(holder) - holder.getUseItemRemainingTicks()) / drawtime;
    }

    @Override
    public MapCodec<Charge> type() {
      return MAP_CODEC;
    }
  }

  /** Property for casting a fishing rod */
  public record Cast() implements RangeSelectItemModelProperty {
    public static final Cast INSTANCE = new Cast();
    public static final MapCodec<Cast> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      // must be a fishing rod, and the player must be fishing
      // does player check first since its the fastest, avoids NBT parsing
      LivingEntity holder = getHolder(owner);
      if (holder instanceof Player player && player.fishing != null && stack.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
        // must be in a hand, but if both hands have fishing rods, must be the one in the main hand
        ItemStack mainhand = holder.getMainHandItem();
        if (mainhand == stack || holder.getOffhandItem() == stack && !mainhand.canPerformAction(ItemAbilities.FISHING_ROD_CAST)) {
          return 1;
        }
      }
      return 0;
    }

    @Override
    public MapCodec<Cast> type() {
      return MAP_CODEC;
    }
  }

  /** Registers the boolean properties, used with {@code minecraft:condition} in client item JSON */
  public static void registerConditionalProperties(RegisterConditionalItemModelPropertyEvent event) {
    event.register(BROKEN_ID, Broken.MAP_CODEC);
  }

  /** Registers the numeric properties, used with {@code minecraft:range_dispatch} in client item JSON */
  public static void registerRangeProperties(RegisterRangeSelectItemModelPropertyEvent event) {
    event.register(AMMO_ID, Ammo.MAP_CODEC);
    event.register(CHARGING_ID, Charging.MAP_CODEC);
    event.register(CHARGE_ID, Charge.MAP_CODEC);
    event.register(CAST_ID, Cast.MAP_CODEC);
  }
}
