package slimeknights.tconstruct.library.recipe.ingredient;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.LoadableIngredientSerializer;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

/** Ingredient matching an {@link net.minecraft.world.item.InstrumentItem} with a particular instrument. */
public class InstrumentIngredient implements ICustomIngredient {
  public static final Identifier ID = TConstruct.getResource("instrument");
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
    // 1.21: instrument lives on the instrument data component rather than a string NBT tag, wrapped in an instrument component since 1.21.5
    InstrumentComponent component = stack.get(DataComponents.INSTRUMENT);
    if (component != null) {
      Holder<Instrument> instrument = component.instrument();
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

  @SuppressWarnings("deprecation")
  @Override
  public Stream<Holder<Item>> items() {
    return Stream.of(item.builtInRegistryHolder());
  }

  /** Displays the item with the instrument set. Since 1.21.2 instruments are a datapack registry, so the instrument is resolved from the current registries. */
  @Override
  public SlotDisplay display() {
    if (instrument != null) {
      Holder<Instrument> holder = RecipeHelper.registryAccess().lookup(Registries.INSTRUMENT).flatMap(registry -> registry.get(instrument)).orElse(null);
      if (holder != null) {
        return new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(item, DataComponentPatch.builder().set(DataComponents.INSTRUMENT, new InstrumentComponent(holder)).build()));
      }
    }
    return new SlotDisplay.ItemSlotDisplay(item);
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
