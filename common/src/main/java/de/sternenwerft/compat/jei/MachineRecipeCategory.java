package de.sternenwerft.compat.jei;

import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeView;
import de.sternenwerft.core.fluid.SwFluidStack;
import de.sternenwerft.recipe.ProcessingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/** JEI-Kategorie einer Maschinen-Rezeptart (3.9). Aufbereitung und Lage kommen aus {@link RecipeView}. */
public class MachineRecipeCategory implements IRecipeCategory<RecipeHolder<ProcessingRecipe>> {

    private final RecipeCategories.Category category;
    private final IRecipeHolderType<ProcessingRecipe> type;
    private final IDrawable icon;
    private final long bucketVolume;

    public MachineRecipeCategory(RecipeCategories.Category category, IRecipeHolderType<ProcessingRecipe> type, IGuiHelper gui,
                                 long bucketVolume) {
        this.category = category;
        this.type = type;
        this.icon = gui.createDrawableItemLike(RecipeView.catalystItem(category));
        this.bucketVolume = bucketVolume;
    }


    @Override
    public IRecipeType<RecipeHolder<ProcessingRecipe>> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(category.titleKey());
    }

    @Override
    public int getWidth() {
        return RecipeView.WIDTH;
    }

    @Override
    public int getHeight() {
        return RecipeView.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    private long platformAmount(SwFluidStack f) {
        return f.amount() * bucketVolume / 1000L;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProcessingRecipe> recipe, IFocusGroup focuses) {
        RecipeView v = RecipeView.of(recipe, category.catalysts());
        int n = 0;
        for (List<ItemStack> alts : v.inputs()) {
            builder.addInputSlot(RecipeView.inputX(n) + 1, RecipeView.inputY(n) + 1).setStandardSlotBackground().addItemStacks(alts);
            n++;
        }
        if (v.mold() != null) {
            builder.addInputSlot(RecipeView.inputX(n) + 1, RecipeView.inputY(n) + 1).setStandardSlotBackground()
                    .addItemStacks(List.of(v.mold()))
                    .addRichTooltipCallback((view, tip) -> tip.add(RecipeView.moldNote()));
            n++;
        }
        for (SwFluidStack f : v.fluidsIn()) {
            fluidSlot(builder.addInputSlot(RecipeView.inputX(n) + 1, RecipeView.inputY(n) + 1), f);
            n++;
        }
        int m = 0;
        for (int i = 0; i < v.outputs().size(); i++) {
            float chance = v.chances().get(i);
            IRecipeSlotBuilder slot = builder.addOutputSlot(RecipeView.outputX(m) + 1, RecipeView.outputY(m) + 1)
                    .setOutputSlotBackground().addItemStacks(List.of(v.outputs().get(i)));
            RecipeView.chanceNote(chance).ifPresent(note -> slot.addRichTooltipCallback((view, tip) -> tip.add(note)));
            m++;
        }
        for (SwFluidStack f : v.fluidsOut()) {
            fluidSlot(builder.addOutputSlot(RecipeView.outputX(m) + 1, RecipeView.outputY(m) + 1), f);
            m++;
        }
    }

    private void fluidSlot(IRecipeSlotBuilder slot, SwFluidStack f) {
        slot.setStandardSlotBackground().addFluidStack(RecipeView.fluid(f.fluid()), platformAmount(f), DataComponentPatch.EMPTY)
                .setFluidRenderer(platformAmount(f), false, 16, 16);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<ProcessingRecipe> recipe, IFocusGroup focuses) {
        RecipeView v = RecipeView.of(recipe, category.catalysts());
        builder.addAnimatedRecipeArrowWidget(Math.max(1, v.ticks())).setPosition(RecipeView.ARROW_X, RecipeView.ARROW_Y);
        builder.addText(v.info(), RecipeView.WIDTH, 10).setPosition(0, RecipeView.TEXT_Y);
    }
}
