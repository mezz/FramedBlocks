package xfacthd.framedblocks.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.client.render.item.ItemRenderHelper;
import xfacthd.framedblocks.common.FBContent;
import xfacthd.framedblocks.common.blockentity.special.PoweredFramingSawBlockEntity;
import xfacthd.framedblocks.common.crafting.*;
import xfacthd.framedblocks.common.menu.FramingSawMenu;
import xfacthd.framedblocks.common.menu.PoweredFramingSawMenu;

import java.util.*;

public class PoweredFramingSawScreen extends AbstractContainerScreen<PoweredFramingSawMenu>
{
    private static final ResourceLocation BACKGROUND = Utils.rl("textures/gui/powered_framing_saw.png");
    public static final Component TITLE_TARGETBLOCK = Utils.translate("title", "powered_saw.target_block");
    public static final MutableComponent MSG_STATUS = Utils.translate("msg", "powered_saw.status");
    public static final Component MSG_STATUS_NO_RECIPE = Utils.translate("msg", "powered_saw.status.no_recipe")
            .withStyle(Style.EMPTY.withColor(0xDD7700));
    public static final Component MSG_STATUS_NO_MATCH = Utils.translate("msg", "powered_saw.status.no_match")
            .withStyle(Style.EMPTY.withColor(0xDD0000));
    public static final Component MSG_STATUS_READY = Utils.translate("msg", "powered_saw.status.ready")
            .withStyle(Style.EMPTY.withColor(0x00DD00));
    public static final Component TOOLTIP_STATUS_NO_RECIPE = Utils.translate("tooltip", "powered_saw.status.no_recipe");
    public static final String TOOLTIP_ENERGY = Utils.translationKey("tooltip", "powered_saw.energy");
    private static final int TITLE_TARGETBLOCK_X = 88;
    private static final int TITLE_TARGETBLOCK_Y = 24;
    private static final int TARGET_STACK_X = 92;
    private static final int TARGET_STACK_Y = 20;
    private static final int STATUS_X = 8;
    private static final int STATUS_Y = 62;
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 18;
    private static final int ENERGY_WIDTH = 14;
    private static final int ENERGY_HEIGHT = 48;
    private static final int ENERGY_U = 176;
    private static final int ENERGY_V = 16;
    public static final int PROGRESS_X = 115;
    public static final int PROGRESS_Y = 46;
    public static final int PROGRESS_WIDTH = 22;
    public static final int PROGRESS_HEIGHT = 16;
    private static final int PROGRESS_U = 176;
    private static final int PROGRESS_V = 0;
    private static final int CROSS_SIZE = 16;
    private static final int CROSS_U = 176;
    private static final int CROSS_V = 64;
    private static final Rect2i EMPTY = new Rect2i(0, 0, 0, 0);

    private final FramingSawRecipeCache cache = FramingSawRecipeCache.get(true);
    private final ItemStack cubeStack = new ItemStack(FBContent.BLOCK_FRAMED_CUBE.get());
    private int targetStackX;
    private int targetStackY;
    private Rect2i statusTooltipArea = EMPTY;
    private List<Component> statusTooltip = List.of();

    public PoweredFramingSawScreen(PoweredFramingSawMenu menu, Inventory inv, Component title)
    {
        super(menu, inv, title);
        imageHeight = 182;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init()
    {
        super.init();

        targetStackX = leftPos + TARGET_STACK_X;
        targetStackY = topPos + TARGET_STACK_Y;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        renderBackground(graphics);

        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int tx = leftPos + TITLE_TARGETBLOCK_X - font.width(TITLE_TARGETBLOCK);
        int ty = topPos + TITLE_TARGETBLOCK_Y;
        graphics.drawString(font, TITLE_TARGETBLOCK, tx, ty, 0x404040, false);

        FramingSawRecipe recipe = menu.getSelectedRecipe();
        FramingSawRecipeMatchResult match = menu.getMatchResult();

        drawRecipeInfo(graphics, recipe, match);
        drawStatus(graphics, recipe, match);
        drawEnergyBar(graphics, mouseX, mouseY);
    }

    private void drawRecipeInfo(GuiGraphics graphics, FramingSawRecipe recipe, FramingSawRecipeMatchResult match)
    {
        Slot inputSlot = menu.getSlot(FramingSawMenu.SLOT_INPUT);
        if (!inputSlot.hasItem())
        {
            int ix = leftPos + inputSlot.x;
            int iy = topPos + inputSlot.y;
            ItemRenderHelper.renderFakeItemTransparent(graphics.pose(), cubeStack, ix, iy, 127);
        }

        if (recipe != null)
        {
            ItemStack result = recipe.getResult();
            ItemRenderHelper.renderFakeItemTransparent(graphics.pose(), result, targetStackX, targetStackY, 127);

            List<FramingSawRecipeAdditive> additives = recipe.getAdditives();
            for (int i = 0; i < FramingSawRecipe.MAX_ADDITIVE_COUNT; i++)
            {
                Slot additiveSlot = menu.getSlot(FramingSawMenu.SLOT_ADDITIVE_FIRST + i);
                if (i >= additives.size())
                {
                    int ax = leftPos + additiveSlot.x;
                    int ay = topPos + additiveSlot.y;
                    graphics.blit(BACKGROUND, ax, ay, CROSS_U, CROSS_V, CROSS_SIZE, CROSS_SIZE);
                }
                else if (!additiveSlot.hasItem())
                {
                    ItemStack[] items = additives.get(i).ingredient().getItems();
                    int t = (int) (System.currentTimeMillis() / 1700) % items.length;
                    int ax = leftPos + additiveSlot.x;
                    int ay = topPos + additiveSlot.y;
                    ItemRenderHelper.renderFakeItemTransparent(graphics.pose(), items[t], ax, ay, 127);
                }
            }

            if (match != null && match.success())
            {
                float progress = (float) menu.getProgress() / (float) PoweredFramingSawBlockEntity.MAX_PROGRESS;
                if (progress > 0F)
                {
                    int width = Math.round(PROGRESS_WIDTH * progress);
                    graphics.blit(BACKGROUND, leftPos + PROGRESS_X, topPos + PROGRESS_Y, PROGRESS_U, PROGRESS_V, width, PROGRESS_HEIGHT);
                }
            }
        }
    }

    private void drawStatus(GuiGraphics graphics, FramingSawRecipe recipe, FramingSawRecipeMatchResult match)
    {
        MutableComponent status = MSG_STATUS.copy();
        int width = -1;
        if (recipe == null)
        {
            status = status.append(MSG_STATUS_NO_RECIPE);
            statusTooltip = List.of(TOOLTIP_STATUS_NO_RECIPE);
            width = font.width(MSG_STATUS_NO_RECIPE);
        }
        else if (match != null && !match.success())
        {
            status = status.append(MSG_STATUS_NO_MATCH);
            statusTooltip = FramingSawScreen.appendRecipeFailure(new ArrayList<>(), cache, menu.getSelectedRecipe(), match, menu);
            width = font.width(MSG_STATUS_NO_MATCH);
        }
        else
        {
            status = status.append(MSG_STATUS_READY);
        }
        int sx = leftPos + STATUS_X;
        int sy = topPos + STATUS_Y + font.lineHeight;
        graphics.drawString(font, status, sx, sy, 0x404040, false);
        statusTooltipArea = width == -1 ? EMPTY : new Rect2i(sx + font.width(MSG_STATUS), sy, width, font.lineHeight);
    }

    private void drawEnergyBar(GuiGraphics graphics, int mouseX, int mouseY)
    {
        float energy = (float) menu.getEnergy() / (float) PoweredFramingSawBlockEntity.ENERGY_CAPACITY;
        int height = (int) (energy * ENERGY_HEIGHT);
        int y = topPos + ENERGY_Y + (ENERGY_HEIGHT - height);
        graphics.blit(BACKGROUND, leftPos + ENERGY_X, y, ENERGY_U, ENERGY_V + (ENERGY_HEIGHT - height), ENERGY_WIDTH, height);

        int minX = leftPos + ENERGY_X;
        int minY = topPos + ENERGY_Y;
        if (mouseX >= minX && mouseX < minX + ENERGY_WIDTH && mouseY >= minY && mouseY < minY + ENERGY_HEIGHT)
        {
            setTooltipForNextRenderPass(Component.translatable(
                    TOOLTIP_ENERGY, menu.getEnergy(), PoweredFramingSawBlockEntity.ENERGY_CAPACITY
            ));
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if (menu.getCarried().isEmpty() && hoveredSlot != null && hoveredSlot.hasItem())
        {
            renderHoveredItemTooltip(graphics, mouseX, mouseY, hoveredSlot.getItem());
            return;
        }

        if (statusTooltipArea.contains(mouseX, mouseY))
        {
            graphics.renderTooltip(font, statusTooltip, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
            statusTooltipArea = EMPTY;
            statusTooltip = List.of();
        }
    }

    private void renderHoveredItemTooltip(GuiGraphics graphics, int mouseX, int mouseY, ItemStack stack)
    {
        //noinspection ConstantConditions
        List<Component> components = new ArrayList<>(getTooltipFromItem(minecraft, stack));
        Optional<TooltipComponent> tooltip = stack.getTooltipImage();

        int material = cache.getMaterialValue(stack.getItem());
        if (material > 0)
        {
            components.add(Component.translatable(FramingSawScreen.TOOLTIP_MATERIAL, material));
        }

        graphics.renderTooltip(font, components, tooltip, stack, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int btn)
    {
        if (mouseX >= targetStackX && mouseX < targetStackX + 18 && mouseY >= targetStackY && mouseY < targetStackY + 18)
        {
            selectRecipe(menu.getCarried());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, btn);
    }

    public void selectRecipe(ItemStack cursorStack)
    {
        if (cursorStack.isEmpty() || cache.getMaterialValue(cursorStack.getItem()) != -1)
        {
            FramingSawRecipe recipe = cache.findRecipeFor(cursorStack);
            if (recipe == menu.getSelectedRecipe())
            {
                return;
            }

            int id = recipe == null ? -1 : cache.getRecipes().indexOf(recipe);
            //noinspection ConstantConditions
            if (menu.clickMenuButton(minecraft.player, id))
            {
                //noinspection ConstantConditions
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            }
        }
    }

    public int getTargetStackX()
    {
        return targetStackX;
    }

    public int getTargetStackY()
    {
        return targetStackY;
    }
}