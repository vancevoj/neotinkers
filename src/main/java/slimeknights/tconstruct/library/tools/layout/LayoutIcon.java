package slimeknights.tconstruct.library.tools.layout;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.common.ItemStackLoadable;
import slimeknights.mantle.data.loadable.common.NBTLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMap;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

import javax.annotation.Nullable;

/**
 * Data holder for a button icon, currently supports item stack icons and pattern icons.
 * Item icons store an {@link ItemStackTemplate} and create the stack on first use, as item stacks cannot be created while data loads
 * (layouts are parsed in a reload listener, before item components are bound) or during datagen.
 */
public abstract class LayoutIcon {
  /** JSON serializer for a layout button icon */
  public static final Serializer SERIALIZER = new Serializer();
  /** JSON keys for item icons, matching Mantle's item stack loadable */
  private static final String KEY_ITEM = "item";
  private static final String KEY_NBT = "nbt";
  private static final String KEY_COMPONENTS = "components";

  /** Empty icon, used primarily as a fallback */
  public static final LayoutIcon EMPTY = new LayoutIcon() {
    @Nullable
    @Override
    public <T> T getValue(Class<T> clazz) {
      return null;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
      buffer.writeEnum(Type.EMPTY);
    }

    @Override
    public JsonObject toJson() {
      return new JsonObject();
    }
  };

  /** Creates a stack icon */
  public static LayoutIcon ofItem(ItemStack stack) {
    if (stack.isEmpty()) {
      return EMPTY;
    }
    // the stack already exists, so keep it as the value
    return new ItemStackIcon(ItemStackTemplate.fromNonEmptyStack(stack), stack);
  }

  /** Creates a stack icon without creating a stack, safe to use before item components are bound */
  public static LayoutIcon ofItem(ItemStackTemplate template) {
    return new ItemStackIcon(template);
  }

  /** Creates a stack icon without creating a stack, safe to use before item components are bound */
  public static LayoutIcon ofItem(ItemLike item) {
    if (item.asItem() == Items.AIR) {
      return EMPTY;
    }
    return new ItemStackIcon(new ItemStackTemplate(item.asItem()));
  }

  /** Creates an icon from a pattern */
  public static LayoutIcon ofPattern(Pattern pattern) {
    return new PatternIcon(pattern);
  }

  /** Gets the value of this icon, done this way to separate the drawing logic out */
  @Nullable
  public abstract <T> T getValue(Class<T> clazz);

  /** Reads the button icon from the buffer */
  public static LayoutIcon read(RegistryFriendlyByteBuf buffer) {
    Type type = buffer.readEnum(Type.class);
    switch (type) {
      case EMPTY: return EMPTY;
      case ITEM: {
        // templates so the stack is created on first use
        return new ItemStackIcon(ItemStackTemplate.STREAM_CODEC.decode(buffer));
      }
      case PATTERN: {
        Pattern pattern = new Pattern(buffer.readIdentifier());
        return new PatternIcon(pattern);
      }
    }
    throw new DecoderException("Invalid LayoutButtonIcon " + type);
  }

  /** Writes this to the packet buffer */
  public abstract void write(RegistryFriendlyByteBuf buffer);

  /** Writes this object to json */
  public abstract JsonObject toJson();

  /** Icon drawing an item stack */
  @VisibleForTesting
  protected static class ItemStackIcon extends LayoutIcon {
    private final ItemStackTemplate template;
    /** Stack created from the template on first use */
    @Nullable
    private ItemStack stack;

    protected ItemStackIcon(ItemStackTemplate template, @Nullable ItemStack stack) {
      this.template = template;
      this.stack = stack;
    }

    protected ItemStackIcon(ItemStackTemplate template) {
      this(template, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == ItemStack.class) {
        // created lazily, by the time anything draws the icon item components are bound
        if (stack == null) {
          stack = template.create();
        }
        return (T) stack;
      }
      if (clazz == ItemStackTemplate.class) {
        return (T) template;
      }
      return null;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
      buffer.writeEnum(Type.ITEM);
      ItemStackTemplate.STREAM_CODEC.encode(buffer, template);
    }

    @Override
    public JsonObject toJson() {
      // same format as Mantle's ItemStackLoadable.OPTIONAL_ITEM_NBT (item, nbt from custom data, other components), without creating a stack
      JsonObject json = new JsonObject();
      json.add(KEY_ITEM, Loadables.ITEM.serialize(template.item().value()));
      DataComponentPatch components = template.components();
      if (components.isPatched(DataComponents.CUSTOM_DATA)) {
        CustomData data = components.getPatch(DataComponents.CUSTOM_DATA);
        if (data != null && !data.isEmpty()) {
          json.add(KEY_NBT, NBTLoadable.ALLOW_STRING.serialize(data.copyTag()));
        }
        components = components.forget(type -> type == DataComponents.CUSTOM_DATA);
      }
      if (!components.isEmpty()) {
        json.add(KEY_COMPONENTS, ItemStackLoadable.serializeComponents(components));
      }
      return json;
    }
  }

  /** Icon drawing a static patttern sprite */
  @RequiredArgsConstructor @VisibleForTesting
  protected static class PatternIcon extends LayoutIcon {
    private final Pattern pattern;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue(Class<T> clazz) {
      if (clazz == Pattern.class) {
        return (T) pattern;
      }
      return null;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buffer) {
      buffer.writeEnum(Type.PATTERN);
      buffer.writeIdentifier(pattern);
    }

    @Override
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      json.addProperty("pattern", pattern.toString());
      return json;
    }
  }

  /** enum of icon types for serialization */
  private enum Type {
    EMPTY,
    ITEM,
    PATTERN
  }

  /** Serializer class */
  protected static class Serializer implements JsonSerializer<LayoutIcon>, JsonDeserializer<LayoutIcon> {
    /**
     * Parses an item icon in the format of Mantle's ItemStackLoadable.OPTIONAL_ITEM_NBT into a template.
     * Layouts load in a reload listener where stacks cannot be created yet, so this does not use the stack loadable.
     */
    private static LayoutIcon parseItem(JsonObject object) {
      Item item = Loadables.ITEM.convert(object.get(KEY_ITEM), KEY_ITEM);
      if (item == Items.AIR) {
        return EMPTY;
      }
      // apply NBT before components, as the stack loadable does
      DataComponentPatch.Builder builder = DataComponentPatch.builder();
      if (object.has(KEY_NBT)) {
        CompoundTag nbt = NBTLoadable.ALLOW_STRING.convert(object.get(KEY_NBT), KEY_NBT);
        if (!nbt.isEmpty()) {
          builder.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
      }
      if (object.has(KEY_COMPONENTS)) {
        DataComponentPatch.SplitResult components = ItemStackLoadable.parseComponents(object.get(KEY_COMPONENTS), TypedMap.EMPTY).split();
        builder.set(components.added());
        for (DataComponentType<?> removed : components.removed()) {
          builder.remove(removed);
        }
      }
      return new ItemStackIcon(new ItemStackTemplate(item, builder.build()));
    }

    @Override
    public LayoutIcon deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject object = GsonHelper.convertToJsonObject(json, "button_icon");
      if (object.has("pattern")) {
        Pattern pattern = new Pattern(JsonHelper.getResourceLocation(object, "pattern"));
        return new PatternIcon(pattern);
      }
      if (object.has(KEY_ITEM)) {
        return parseItem(object);
      }
      // not sure why this would be needed, but might as well
      if (object.entrySet().isEmpty()) {
        return EMPTY;
      }
      throw new JsonSyntaxException("LayoutButtonIcon must have either pattern or item");
    }

    @Override
    public JsonElement serialize(LayoutIcon icon, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
      return icon.toJson();
    }
  }
}
