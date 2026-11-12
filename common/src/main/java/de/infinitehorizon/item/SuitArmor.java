package de.infinitehorizon.item;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.data.BalanceManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Locale;

/**
 * Rüstungsmaterial der Raumanzüge (9.1): Werte wie die Vanilla-Rüstung aus {@code armor_like} in suits.json
 * (T1 Eisen: Schutz, Haltbarkeit, Verzauberbarkeit), Reparatur über den Item-Tag
 * {@code infinite_horizon:repairs_<anzug>} (aus {@code repair_item}), eigenes Equipment-Asset
 * {@code infinite_horizon:<anzug>}.
 */
public final class SuitArmor {

    private SuitArmor() {
    }

    /** Material für den Anzug {@code suitId} (Schlüssel in suits.json, z. B. {@code pressure_suit}). */
    @SuppressWarnings("unchecked")
    public static ArmorMaterial material(String suitId) {
        JsonObject o = BalanceManager.getOrBundled().entry("suits", suitId)
                .orElseThrow(() -> new IllegalStateException("suits.json: " + suitId + " fehlt"));
        ArmorMaterial base = vanilla(o.get("armor_like").getAsString());
        TagKey<net.minecraft.world.item.Item> repair = TagKey.create(Registries.ITEM, InfiniteHorizon.id("repairs_" + suitId));
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(
                (ResourceKey<? extends net.minecraft.core.Registry<EquipmentAsset>>) EquipmentAssets.ROOT_ID,
                InfiniteHorizon.id(suitId));
        return new ArmorMaterial(base.durability(), base.defense(), base.enchantmentValue(), base.equipSound(),
                base.toughness(), base.knockbackResistance(), repair, asset);
    }

    static ArmorMaterial vanilla(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "leather" -> ArmorMaterials.LEATHER;
            case "copper" -> ArmorMaterials.COPPER;
            case "chainmail" -> ArmorMaterials.CHAINMAIL;
            case "gold" -> ArmorMaterials.GOLD;
            case "diamond" -> ArmorMaterials.DIAMOND;
            case "netherite" -> ArmorMaterials.NETHERITE;
            default -> ArmorMaterials.IRON;
        };
    }
}
