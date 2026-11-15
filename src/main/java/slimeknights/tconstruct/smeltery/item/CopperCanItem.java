package slimeknights.tconstruct.smeltery.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.FluidValues;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Fluid container holding 1 ingot of fluid
 */
public class CopperCanItem extends Item {
  private static final String TAG_FLUID = "fluid";
  private static final String TAG_FLUID_TAG = "fluid_tag";

  public CopperCanItem(Properties properties) {
    super(properties);
  }

  @Nullable
  @Override
  public ItemStackTemplate getCraftingRemainder(ItemInstance instance) {
    if (getFluid(instance) != Fluids.EMPTY) {
      return new ItemStackTemplate(this);
    }
    return null;
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
    Fluid fluid = getFluid(stack);
    if (fluid != Fluids.EMPTY) {
      MutableComponent text = getFluidStack(stack, FluidValues.INGOT).getHoverName().plainCopy();
      tooltip.accept(Component.translatable(this.getDescriptionId() + ".contents", text).withStyle(ChatFormatting.GRAY));
      if (flag.isAdvanced()) {
        tooltip.accept(Component.translatable(TankItem.FLUID_ID, Loadables.FLUID.getKey(fluid)).withStyle(ChatFormatting.DARK_GRAY));
      }
    } else {
      tooltip.accept(Component.translatable(this.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
  }

  /** Removes the fluid from the given stack */
  public static void removeFluid(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      nbt.remove(TAG_FLUID);
      nbt.remove(TAG_FLUID_TAG);
      if (nbt.isEmpty()) {
        stack.remove(DataComponents.CUSTOM_DATA);
      } else {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
      }
    }
  }

  /** Sets the fluid on the given stack whether or not its valiid */
  private static void setFluidInternal(ItemStack stack, Identifier fluid, @Nullable CompoundTag fluidTag) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
      nbt.putString(TAG_FLUID, fluid.toString());
      if (fluidTag != null) {
        nbt.put(TAG_FLUID_TAG, fluidTag.copy());
      } else {
        nbt.remove(TAG_FLUID_TAG);
      }
    });
  }


  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Identifier fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid.equals(BuiltInRegistries.FLUID.getDefaultKey())) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, fluid, fluidTag);
    }
    return stack;
  }
  /** Sets the fluid on the given stack */
  @SuppressWarnings("deprecation")
  public static ItemStack setFluid(ItemStack stack, Fluid fluid, @Nullable CompoundTag fluidTag) {
    // if empty, try to remove the NBT, helps with recipes
    if (fluid == Fluids.EMPTY) {
      removeFluid(stack);
    } else {
      setFluidInternal(stack, BuiltInRegistries.FLUID.getKey(fluid), fluidTag);
    }
    return stack;
  }

  /** Sets the fluid on the given stack, preserving the fluid's data components */
  public static ItemStack setFluid(ItemStack stack, FluidStack fluid) {
    return setFluid(stack, fluid.getFluid(), serializeComponents(fluid.getComponentsPatch()));
  }

  /** Serializes a fluid component patch into the compound stored under {@link #TAG_FLUID_TAG}, or null if empty */
  @Nullable
  private static CompoundTag serializeComponents(DataComponentPatch patch) {
    if (patch.isEmpty()) {
      return null;
    }
    Tag encoded = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, patch).result().orElse(null);
    if (encoded instanceof CompoundTag compound && !compound.isEmpty()) {
      return compound;
    }
    return null;
  }

  /** Deserializes the stored component patch from the given stack */
  private static DataComponentPatch deserializeComponents(ItemStack stack) {
    CompoundTag fluidTag = getFluidTag(stack);
    if (fluidTag != null) {
      return DataComponentPatch.CODEC.parse(NbtOps.INSTANCE, fluidTag).result().orElse(DataComponentPatch.EMPTY);
    }
    return DataComponentPatch.EMPTY;
  }

  /** Gets the contained fluid as a stack with its stored components, using the given amount */
  public static FluidStack getFluidStack(ItemStack stack, int amount) {
    Fluid fluid = getFluid(stack);
    if (fluid == Fluids.EMPTY) {
      return FluidStack.EMPTY;
    }
    Holder<Fluid> holder = BuiltInRegistries.FLUID.wrapAsHolder(fluid);
    return new FluidStack(holder, amount, deserializeComponents(stack));
  }

  /** Gets the fluid from the given stack */
  public static Fluid getFluid(ItemStack stack) {
    return getFluid((DataComponentGetter) stack);
  }

  /** Gets the fluid from the given item components */
  private static Fluid getFluid(DataComponentGetter stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      if (nbt.get(TAG_FLUID) instanceof StringTag(String fluidName)) {
        Identifier location = Identifier.tryParse(fluidName);
        if (location != null && BuiltInRegistries.FLUID.containsKey(location)) {
          Fluid fluid = BuiltInRegistries.FLUID.getValue(location);
          if (fluid != null) {
            return fluid;
          }
        }
      }
    }
    return Fluids.EMPTY;
  }

  /** Adds filled variants of the copper can to the given consumer */
  @SuppressWarnings("deprecation")
  public static void addFilledVariants(Consumer<ItemStack> output) {
    BuiltInRegistries.FLUID.listElements().filter(holder -> {
      Fluid fluid = holder.value();
      return fluid.isSource(fluid.defaultFluidState()) && !holder.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS);
    }).forEachOrdered(holder -> {
      output.accept(CopperCanItem.setFluid(new ItemStack(TinkerSmeltery.copperCan), holder.key().identifier(), null));
    });
  }

  /** Gets the fluid NBT from the given stack */
  @Nullable
  public static CompoundTag getFluidTag(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      CompoundTag nbt = data.copyTag();
      if (nbt.get(TAG_FLUID_TAG) instanceof CompoundTag fluidTag) {
        return fluidTag;
      }
    }
    return null;
  }

  /**
   * Gets a string variant name for the given stack
   * @param stack  Stack instance to check
   * @return  String variant name
   */
  public static String getSubtype(ItemStack stack) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    if (data != null) {
      return data.copyTag().getStringOr(TAG_FLUID, "");
    }
    return "";
  }
}
