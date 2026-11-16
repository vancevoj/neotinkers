package slimeknights.tconstruct.library.json.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.SingleEntryContainerBase;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import slimeknights.mantle.data.loadable.LoadableCodec;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.library.materials.RandomMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IToolPart;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Entry for a random tool part from a list with a random material */
public class ToolPartLootEntry extends SingleEntryContainerBase {
  /** Codec for this entry */
  public static final MapCodec<ToolPartLootEntry> CODEC = RecordCodecBuilder.mapCodec(inst ->
    inst.group(
        TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(e -> e.tag),
        new LoadableCodec<>(RandomMaterial.LOADER).fieldOf("material").forGetter(e -> e.material))
      .and(uniformFields(inst))
      .apply(inst, ToolPartLootEntry::new));

  private final TagKey<Item> tag;
  private final RandomMaterial material;

  protected ToolPartLootEntry(TagKey<Item> tag, RandomMaterial material, int weight, int quality, Optional<Holder<LootItemCondition>> condition, Optional<Holder<LootItemFunction>> modifier) {
    super(weight, quality, condition, modifier);
    this.tag = tag;
    this.material = material;
  }

  @Override
  public MapCodec<ToolPartLootEntry> codec() {
    return CODEC;
  }

  @SuppressWarnings("deprecation")
  @Override
  protected void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
    List<IToolPart> options = RegistryHelper.getTagValueStream(BuiltInRegistries.ITEM, tag)
      .flatMap(item -> item instanceof IToolPart mat ? Stream.of(mat) : Stream.empty()).toList();
    if (!options.isEmpty()) {
      RandomSource random = context.getRandom();
      IToolPart choice = options.get(random.nextInt(options.size()));
      MaterialVariantId material = this.material.getMaterial(choice.getStatType(), random);
      if (choice.canUseMaterial(material.getId())) {
        consumer.accept(choice.withMaterial(material));
      }
    }
  }


  /* Builders */

  /** Creates a builder with the given material */
  public static UniformContainerBase.Builder<?> entry(TagKey<Item> tag, RandomMaterial material) {
    return simpleBuilder((weight, quality, condition, modifier) -> new ToolPartLootEntry(tag, material, weight, quality, condition, modifier));
  }

  /** Creates a builder for a fixed material */
  public static UniformContainerBase.Builder<?> fixed(TagKey<Item> tag, MaterialVariantId material) {
    return entry(tag, RandomMaterial.fixed(material));
  }
}
