package slimeknights.tconstruct.library.tools.nbt;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import slimeknights.tconstruct.library.recipe.RecipeResult;

import javax.annotation.Nullable;

/** Helper which contains a lazily loaded tool stack, used for recipe output to reduce NBT parsing */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LazyToolStack {
  @Nullable
  private ItemStack stack;
  @Nullable
  private ToolStack tool;
  @Getter
  private final int size;

  /** Creates from a stack, lazily loading the tool stack */
  public static LazyToolStack from(ItemStack stack) {
    return new LazyToolStack(stack, null, stack.getCount());
  }

  /** Creates from a tool with the given count, lazily loading the stack */
  public static LazyToolStack from(ToolStack tool, int count) {
    return new LazyToolStack(null, tool, count);
  }

  /** Creates a success for a tinker station or modifier worktable recipe */
  public static RecipeResult<LazyToolStack> success(ToolStack tool, int count) {
    return RecipeResult.success(LazyToolStack.from(tool, count));
  }

  /** Creates a success for a tinker station or modifier worktable recipe */
  public static RecipeResult<LazyToolStack> success(ItemStack stack) {
    return RecipeResult.success(LazyToolStack.from(stack));
  }

  /** Gets the item inside this stack without a need to resolving. */
  public Item getItem() {
    if (stack != null) {
      return stack.getItem();
    }
    if (tool != null) {
      return tool.getItem();
    }
    return Items.AIR;
  }

  /** Checks if the stack has the given tag without resolving. */
  public boolean hasTag(TagKey<Item> tag) {
    if (stack != null) {
      return stack.is(tag);
    }
    if (tool != null) {
      return tool.hasTag(tag);
    }
    return false;
  }

  /** Gets the tool for this instance */
  public ToolStack getTool() {
    if (tool == null) {
      assert stack != null;
      tool = ToolStack.from(stack);
    }
    return tool;
  }

  /** Gets the item stack for this instance */
  public ItemStack getStack() {
    if (stack == null) {
      assert tool != null;
      stack = tool.createStack(size);
    }
    return stack;
  }
}
