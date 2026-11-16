package slimeknights.tconstruct.library.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import slimeknights.mantle.client.render.FluidSprites;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GuiUtil {
  /**
   * Draws the background of a container
   * @param graphics    Graphics context
   * @param screen      Parent screen
   * @param background  Background location
   */
  public static void drawBackground(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, Identifier background) {
    // 26.3: the GUI has no shader color state any more, so a fluid tint drawn earlier can no longer make the background transparent
    graphics.blit(RenderPipelines.GUI_TEXTURED, background, screen.getLeftPos(), screen.getTopPos(), 0, 0, screen.getImageWidth(), screen.getImageHeight(), 256, 256);
  }

  /**
   * Checks if the given area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the area is hovered
   */
  public static boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
    return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
  }

  /**
   * Checks if the given tank area is hovered
   * @param mouseX    Mouse X position
   * @param mouseY    Mouse Y position
   * @param amount    Current tank amount
   * @param capacity  Tank capacity
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @return  True if the tank is hovered, false otherwise
   */
  public static boolean isTankHovered(int mouseX, int mouseY, int amount, int capacity, int x, int y, int width, int height) {
    // check X position first, its easier
    if (mouseX < x || mouseX > x + width || mouseY > y + height) {
      return false;
    }
    // next, try height
    int topHeight = height - (height * amount / capacity);
    return mouseY > y + topHeight;
  }

  /**
   * Renders a fluid tank with a partial fluid level
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @param depth     Tank depth, unused since 26.3 as the GUI has no depth. Later draws layer above earlier ones.
   */
  public static void renderFluidTank(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack, int capacity, int x, int y, int width, int height, int depth) {
    renderFluidTank(graphics, screen, stack, stack.getAmount(), capacity, x, y, width, height, depth);
  }

  /**
   * Renders a fluid tank with a partial fluid level and an amount override
   * @param graphics  Graphics context
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param capacity  Tank capacity, determines height
   * @param x         Tank X position
   * @param y         Tank Y position
   * @param width     Tank width
   * @param height    Tank height
   * @param depth     Tank depth, unused since 26.3
   */
  public static void renderFluidTank(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack, int amount, int capacity, int x, int y, int width, int height, int depth) {
    if(!stack.isEmpty() && capacity > 0) {
      int maxY = y + height;
      int fluidHeight = Math.min(height * amount / capacity, height);
      renderTiledFluid(graphics, screen, stack, x, maxY - fluidHeight, width, fluidHeight, depth);
    }
  }

  /**
   * Colors and renders a fluid sprite tiled over the given area. x/y are relative to the screen's leftPos/topPos.
   * Partial tiles at the right and bottom edge are cropped by a scissor instead of being squished.
   * @param graphics  Graphics context
   * @param screen    Parent screen
   * @param stack     Fluid stack
   * @param x         Fluid X
   * @param y         Fluid Y
   * @param width     Fluid width
   * @param height    Fluid height
   * @param depth     Fluid depth, unused since 26.3
   */
  public static void renderTiledFluid(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, FluidStack stack, int x, int y, int width, int height, int depth) {
    if (stack.isEmpty() || width <= 0 || height <= 0) {
      return;
    }
    // 26.3: fluid textures and tint come from the vanilla fluid model, works for every fluid with a fluid model
    FluidSprites sprites = FluidSprites.of(stack);
    TextureAtlasSprite sprite = sprites.still();
    int color = sprites.color();
    if ((color & 0xFF000000) == 0) {
      color |= 0xFF000000; // many fluids only provide RGB; treat a missing alpha as opaque
    }
    int startX = x + screen.getLeftPos();
    int startY = y + screen.getTopPos();
    int spriteW = Math.max(1, sprite.contents().width());
    int spriteH = Math.max(1, sprite.contents().height());
    // tile the still sprite over the area, the scissor crops the edge tiles to the area bounds
    graphics.enableScissor(startX, startY, startX + width, startY + height);
    for (int dx = 0; dx < width; dx += spriteW) {
      for (int dy = 0; dy < height; dy += spriteH) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, startX + dx, startY + dy, spriteW, spriteH, color);
      }
    }
    graphics.disableScissor();
  }

  /**
   * Renders a texture atlas sprite tiled over the given area
   * @param graphics    Graphics context
   * @param screen      Parent screen
   * @param sprite      Sprite to render
   * @param x           X position to render, relative to the screen's leftPos
   * @param y           Y position to render, relative to the screen's topPos
   * @param width       Render width
   * @param height      Render height
   * @param depth       Render depth, unused since 26.3
   * @param upsideDown  If true, flips the sprite
   */
  public static void renderTiledTextureAtlas(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, TextureAtlasSprite sprite, int x, int y, int width, int height, int depth, boolean upsideDown) {
    Identifier atlas = sprite.atlasLocation();
    // tile vertically
    float u1 = sprite.getU0();
    float v1 = sprite.getV0();
    int spriteHeight = sprite.contents().height();
    int spriteWidth = sprite.contents().width();
    int startX = x + screen.getLeftPos();
    int startY = y + screen.getTopPos();
    do {
      int renderHeight = Math.min(spriteHeight, height);
      height -= renderHeight;
      // getV takes a normalized 0..1 coordinate
      float v2 = sprite.getV((float) renderHeight / spriteHeight);

      // we need to draw the quads per width too
      int x2 = startX;
      int widthLeft = width;
      // tile horizontally
      do {
        int renderWidth = Math.min(spriteWidth, widthLeft);
        widthLeft -= renderWidth;

        float u2 = sprite.getU((float) renderWidth / spriteWidth);
        if(upsideDown) {
          // FIXME: I think this causes tiling errors, look into it
          graphics.blit(atlas, x2, startY, x2 + renderWidth, startY + renderHeight, u1, u2, v2, v1);
        } else {
          graphics.blit(atlas, x2, startY, x2 + renderWidth, startY + renderHeight, u1, u2, v1, v2);
        }
        x2 += renderWidth;
      } while(widthLeft > 0);

      startY += renderHeight;
    } while(height > 0);
  }

  /**
   * Draws an upwards progress bar. TODO: is this just {@link slimeknights.mantle.client.screen.ScalableElementScreen}?
   * @param element   Element to draw
   * @param x         X position to start
   * @param y         Y position to start
   * @param progress  Progress between 0 and 1
   */
  public static void drawProgressUp(GuiGraphicsExtractor graphics, ElementScreen element, int x, int y, float progress) {
    int height;
    if (progress > 1) {
      height = element.h;
    } else if (progress < 0) {
      height = 0;
    } else {
      // add an extra 0.5 so it rounds instead of flooring
      height = (int)(progress * element.h + 0.5);
    }
    // amount to offset element by for the height
    int deltaY = element.h - height;
    graphics.blit(RenderPipelines.GUI_TEXTURED, element.texture, x, y + deltaY, element.x, element.y + deltaY, element.w, height, element.texW, element.texH);
  }

  /**
   * Renders a highlight overlay for the given area.
   * 26.3: the old color mask trick to keep the alpha channel is gone, this is a plain translucent fill drawn above earlier elements.
   * @param graphics  Graphics instance
   * @param x         Element X position
   * @param y         Element Y position
   * @param width     Element width
   * @param height    Element height
   */
  public static void renderHighlight(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
    graphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
  }

  /** Renders a pattern at the given location */
  public static void renderPattern(GuiGraphicsExtractor graphics, Pattern pattern, int x, int y) {
    // patterns stay on the block atlas, see TinkerSpriteSourceProvider
    TextureAtlasSprite sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS, pattern.getTexture()));
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16);
  }
}
