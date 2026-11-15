package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching an {@link net.minecraft.world.item.InstrumentItem} with a particular instrument. */
public class InstrumentIngredient implements ICustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("instrument");
  /** Loadable for parsing and serializing this ingredient */
  public static final RecordLoadable<InstrumentIngredient> LOADABLE = RecordLoadable.create(
    Loadables.ITEM.requiredField("item", i -> i.item),
    Loadables.resourceKey(Registries.INSTRUMENT).nullableField("instrument", i -> i.instrument),
    TinkerLoadables.INSTRUMENT_TAGS.nullableField("ignore", i -> i.ignore),
    InstrumentIngredient::new
  ).validate((ingredient, error) -> {
    if (ingredient.instrument == null && ingredient.ignore == null) {
      throw error.create("Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
    }
    return ingredient;
  });
  /** Ingredient type instance, registered under {@link #ID} */
  public static final IngredientType<InstrumentIngredient> TYPE = new LoadableIngredientSerializer<>(LOADABLE).type();

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;
  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    this.item = item.asItem();
    this.instrument = instrument;
    // instrument takes precedence over ignore, matching the old JSON parsing
    this.ignore = instrument == null ? ignore : null;
  }

  /** Creates a new instance matching the given instrument */
  public static InstrumentIngredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument, null);
  }

  /** Creates a new instance ignoring the given tag */
  public static InstrumentIngredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore);
  }

  @Override
  public boolean isSimple() {
    return false;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    // 1.21: instrument lives on the instrument data component rather than a string NBT tag
    Holder<Instrument> instrument = stack.get(DataComponents.INSTRUMENT);
    if (instrument != null) {
      // if we just want the instrument to match, can just compare by key
      if (this.instrument != null) {
        return instrument.is(this.instrument);
      }
      assert this.ignore != null;
      // must be registered (not an inline instrument) and not in the tag
      return instrument.unwrapKey().isPresent() && !instrument.is(ignore);
    }
    // if no instrument, its fine as long as we don't have a specific instrument
    return this.instrument == null;
  }

  @Override
  public Stream<ItemStack> getItems() {
    ItemStack stack = new ItemStack(item);
    // set the instrument on the stack
    if (instrument != null) {
      BuiltInRegistries.INSTRUMENT.getHolder(instrument).ifPresent(holder -> stack.set(DataComponents.INSTRUMENT, holder));
    }
    return Stream.of(stack);
  }

  @Override
  public IngredientType<?> getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    return other instanceof InstrumentIngredient ingredient && this.item == ingredient.item
      && Objects.equals(this.instrument, ingredient.instrument) && Objects.equals(this.ignore, ingredient.ignore);
  }

  @Override
  public int hashCode() {
    return Objects.hash(item, instrument, ignore);
  }
}
