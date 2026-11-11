package de.sternenwerft.compat.rei;

import de.sternenwerft.compat.ClientRecipes;
import de.sternenwerft.compat.RecipeCategories;
import de.sternenwerft.compat.RecipeKinds;
import de.sternenwerft.compat.RecipeView;
import de.sternenwerft.compat.RecipeViewerHooks;
import de.sternenwerft.data.BalanceManager;
import de.sternenwerft.recipe.ProcessingRecipe;
import de.sternenwerft.registry.ModBlocks;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import de.sternenwerft.client.FluidColors;
import dev.architectury.fluid.FluidStack;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.entry.renderer.EntryRendererRegistry;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * REI-Plugin für den Client (3.9): Kategorien, Werkbänke (Maschinen) und Anzeigen aus {@link ClientRecipes}.
 * Fabric: Entry-Point {@code rei_client}; NeoForge: Unterklasse mit {@code @REIPluginClient}. Wird nur geladen, wenn REI
 * installiert ist.
 */
public class SwReiClientPlugin implements REIClientPlugin {

    /** REI-Kategorie der Vanilla-Schmelzrezepte (Standard-Plugin von REI). */
    private static final CategoryIdentifier<?> VANILLA_SMELTING = CategoryIdentifier.of("minecraft", "plugins/smelting");

    private static List<RecipeCategories.Category> categories() {
        return RecipeCategories.build(BalanceManager.getOrBundled(), RecipeKinds.all().stream().map(ProcessingRecipe.Kind::name).toList());
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        for (RecipeCategories.Category c : categories()) {
            registry.add(new MachineDisplayCategory(c));
            if (!c.catalysts().isEmpty()) {
                registry.addWorkstations(MachineRecipeDisplay.categoryOf(c.type()), EntryStacks.of(RecipeView.catalystItem(c)));
            }
            // Maschinenrezepte lassen sich nicht ins Fenster übertragen: kein „+“/„!“-Knopf
            registry.removePlusButton(MachineRecipeDisplay.categoryOf(c.type()));
        }
        try {
            // Vanilla-Schmelzrezepte laufen über die Vanilla-Kategorie (3.9); fehlt das REI-Standard-Plugin, entfällt das
            registry.addWorkstations(VANILLA_SMELTING, EntryStacks.of(ModBlocks.ELECTRIC_FURNACE.get()));
        } catch (RuntimeException e) {
            // keine Vanilla-Kategorie vorhanden
        }
        RecipeViewerHooks.active = "rei";
        RecipeViewerHooks.showCategory = name -> ViewSearchBuilder.builder().addCategory(MachineRecipeDisplay.categoryOf(name)).open();
        // Taste H über einem Item in der REI-Liste oder einem Rezept (24.1)
        RecipeViewerHooks.hoveredStack = SwReiClientPlugin::hoveredStack;
        RecipeViewerHooks.typing = () -> {
            var field = REIRuntime.getInstance().getSearchTextField();
            return field != null && field.isFocused();
        };
    }

    private static ItemStack hoveredStack() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui.screen();
        if (screen == null) {
            return ItemStack.EMPTY;
        }
        Point mouse = new Point((int) mc.mouseHandler.getScaledXPos(mc.getWindow()), (int) mc.mouseHandler.getScaledYPos(mc.getWindow()));
        EntryStack<?> stack = ScreenRegistry.getInstance().getFocusedStack(screen, mouse);
        return stack != null && stack.getType() == VanillaEntryTypes.ITEM ? stack.castValue() : ItemStack.EMPTY;
    }

    /**
     * Fluide als Farbfläche wie in den Maschinen-GUIs: Architectury zeichnet unter Fabric in 26.3 nur eigene Fluide
     * (sonst bleibt der Slot leer). Für unsere Fluide sowie Wasser und Lava; Tooltip bleibt der von REI.
     */
    @Override
    public void registerEntryRenderers(EntryRendererRegistry registry) {
        registry.register(VanillaEntryTypes.FLUID, (entry, last) -> {
            Identifier id = BuiltInRegistries.FLUID.getKey(entry.getValue().getFluid());
            if (!FluidColors.known(id)) {
                return last;
            }
            return new EntryRenderer<>() {
                @Override
                public void render(EntryStack<FluidStack> stack, GuiGraphics g, Rectangle b, int mouseX, int mouseY, float delta) {
                    g.fill(b.x, b.y, b.x + b.width, b.y + b.height, FluidColors.of(id));
                }

                @Override
                public Tooltip getTooltip(EntryStack<FluidStack> stack, TooltipContext context) {
                    return last.getTooltip(stack, context);
                }
            };
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        int count = 0;
        for (ProcessingRecipe.Kind kind : RecipeKinds.all()) {
            for (RecipeHolder<ProcessingRecipe> h : ClientRecipes.of(kind)) {
                registry.add(MachineRecipeDisplay.of(h));
                count++;
            }
        }
        RecipeViewerHooks.recipeCount = count;
    }
}
