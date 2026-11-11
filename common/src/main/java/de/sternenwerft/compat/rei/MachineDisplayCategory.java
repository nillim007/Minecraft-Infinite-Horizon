package de.sternenwerft.compat.rei;

import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeView;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** REI-Kategorie einer Maschinen-Rezeptart (3.9); Lage wie im JEI-Plugin ({@link RecipeView}). */
public class MachineDisplayCategory implements DisplayCategory<MachineRecipeDisplay> {

    private static final int BORDER = 5;
    // Textfarbe der Zeile mit Dauer und Energie (hell/dunkel wie Vanilla-GUI-Text)
    private static final int TEXT_LIGHT = 0xFF404040;
    private static final int TEXT_DARK = 0xFFBBBBBB;

    private final RecipeCategories.Category category;

    public MachineDisplayCategory(RecipeCategories.Category category) {
        this.category = category;
    }

    @Override
    public CategoryIdentifier<? extends MachineRecipeDisplay> getCategoryIdentifier() {
        return MachineRecipeDisplay.categoryOf(category.type());
    }

    @Override
    public Component getTitle() {
        return Component.translatable(category.titleKey());
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(RecipeView.catalystItem(category));
    }

    @Override
    public int getDisplayHeight() {
        return RecipeView.HEIGHT + 2 * BORDER;
    }

    @Override
    public int getDisplayWidth(MachineRecipeDisplay display) {
        return RecipeView.WIDTH + 2 * BORDER;
    }

    @Override
    public List<Widget> setupDisplay(MachineRecipeDisplay display, Rectangle bounds) {
        List<Widget> w = new ArrayList<>();
        w.add(Widgets.createRecipeBase(bounds));
        int ox = bounds.x + BORDER;
        int oy = bounds.y + BORDER;
        int n = 0;
        for (EntryIngredient in : display.getInputEntries()) {
            w.add(Widgets.createSlot(new Point(ox + RecipeView.inputX(n) + 1, oy + RecipeView.inputY(n) + 1)).entries(in).markInput());
            n++;
        }
        w.add(Widgets.createArrow(new Point(ox + RecipeView.ARROW_X, oy + RecipeView.ARROW_Y)));
        int m = 0;
        for (EntryIngredient out : display.getOutputEntries()) {
            w.add(Widgets.createSlot(new Point(ox + RecipeView.outputX(m) + 1, oy + RecipeView.outputY(m) + 1)).entries(out).markOutput());
            m++;
        }
        w.add(Widgets.createLabel(new Point(ox, oy + RecipeView.TEXT_Y), display.view().info())
                .leftAligned().noShadow().color(TEXT_LIGHT, TEXT_DARK));
        return w;
    }
}
