package slimeknights.tconstruct.shared.command.subcommand.generate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.FalseCondition;
import net.minecraftforge.common.crafting.conditions.ICondition;
import slimeknights.mantle.command.MantleCommand;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ItemCastingRecipe;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static slimeknights.mantle.util.JsonHelper.DEFAULT_GSON;

/** Command to generate recipes that disable smelting ingots in a furnace */
public class RemoveRecipesCommand {
  private static final String KEY_SUCCESS = TConstruct.makeTranslationKey("command", "generate.remove_recipes");
  private static final DynamicCommandExceptionType ITEM_NOT_FOUND = new DynamicCommandExceptionType(id -> TConstruct.makeTranslation("command", "item.not_found", id));

  /**
   * Registers this sub command with the root command
   *
   * @param subCommand Command builder
   * @param context    Context to fetch the recipe type argument
   */
  public static void register(LiteralArgumentBuilder<CommandSourceStack> subCommand, CommandBuildContext context) {
    subCommand.requires(sender -> sender.hasPermission(MantleCommand.PERMISSION_GAME_COMMANDS))
      .then(Commands.argument("recipe_type", ResourceArgument.resource(context, Registries.RECIPE_TYPE))
        .then(Commands.argument("items", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.ITEM))
          .executes(RemoveRecipesCommand::runTag)
        .then(Commands.literal("castable")
          .executes(RemoveRecipesCommand::runCastable))));
  }

  /** Gets the predicate from an item resource or tag key */
  private static Predicate<ItemStack> getPredicate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    Either<ResourceKey<Item>, TagKey<Item>> items = ResourceOrTagKeyArgument.getResourceOrTagKey(context, "items", Registries.ITEM, ITEM_NOT_FOUND).unwrap();
    return items.map(
        key -> stack -> stack.getItem().builtInRegistryHolder().is(key),
        tag -> stack -> stack.is(tag));
  }

  /** Runs the command */
  private static int runTag(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    long startTime = System.nanoTime();
    return run(context, ResourceArgument.getResource(context, "recipe_type", Registries.RECIPE_TYPE).get(), getPredicate(context), startTime);
  }

  /** Runs the command */
  private static int runCastable(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    long startTime = System.nanoTime();
    Holder<RecipeType<?>> recipeType = ResourceArgument.getResource(context, "recipe_type", Registries.RECIPE_TYPE);
    Predicate<ItemStack> predicate = getPredicate(context);

    // get a list of all matching items that support casting; don't want to remove furnace recipes if they don't support casting
    ServerLevel level = context.getSource().getLevel();
    RegistryAccess access = level.registryAccess();
    Collection<Item> castable = new ArrayList<>();
    for (ICastingRecipe recipe : level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.CASTING_TABLE.get())) {
      ItemStack result = recipe.getResultItem(access);
      if (predicate.test(result)) {
        // if it's an item casting, check if it's a tag
        if (recipe instanceof ItemCastingRecipe itemCasting) {
          // not a better way right now to check if it's a tag output, so just serialize then fetch the tag from JSON
          JsonElement element = itemCasting.getResult().serialize(false);
          if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            if (json.has("tag")) {
              for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(Loadables.ITEM_TAG.getIfPresent(json, "tag"))) {
                castable.add(holder.get());
              }
              continue;
            }
          }
        }
        // not a tag output? add the single item
        castable.add(result.getItem());
      }
    }
    return run(context, recipeType.get(), stack -> castable.contains(stack.getItem()) && predicate.test(stack), startTime);
  }

  /** Runs the command */
  @SuppressWarnings("unchecked")  // not like we are using the generics at all
  private static <C extends Container, T extends Recipe<C>> int run(CommandContext<CommandSourceStack> context, RecipeType<?> recipeType, Predicate<ItemStack> removeIf, long startTime) {
    // iterate all recipes for the type storing recipes that craft the tag
    ServerLevel level = context.getSource().getLevel();
    RegistryAccess access = level.registryAccess();
    List<ResourceLocation> toRemove = new ArrayList<>();
    for (Recipe<?> recipe : context.getSource().getLevel().getRecipeManager().getAllRecipesFor((RecipeType<T>) recipeType)) {
      ItemStack result = recipe.getResultItem(access);
      if (removeIf.test(result)) {
        toRemove.add(recipe.getId());
      }
    }

    // determine the path for the resulting datapack
    Path pack = GeneratePackUtil.getDatapackPath(level.getServer());
    GeneratePackUtil.saveMcmeta(pack);

    // create the object for removing recipes
    JsonObject remove = new JsonObject();
    remove.add("conditions", CraftingHelper.serialize(new ICondition[]{FalseCondition.INSTANCE}));
    String removeJson = DEFAULT_GSON.toJson(remove);

    int successes = 0;
    Path data = pack.resolve(PackType.SERVER_DATA.getDirectory());
    for (ResourceLocation id : toRemove) {
      Path path = data.resolve(id.getNamespace() + "/recipes/" + id.getPath() + ".json");
      try {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
          writer.write(removeJson);
          successes += 1;
        }
      } catch(IOException e){
        TConstruct.LOG.error("Couldn't save recipe {}", id, e);
      }
    }

    // send success
    int successFinal = successes;
    float time = (System.nanoTime() - startTime) / 1000000f;
    context.getSource().sendSuccess(() -> Component.translatable(KEY_SUCCESS, successFinal, time, GeneratePackUtil.getOutputComponent(pack)), true);
    return successes;
  }
}
