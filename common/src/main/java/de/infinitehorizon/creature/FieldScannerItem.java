package de.infinitehorizon.creature;

import com.google.gson.JsonObject;
import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.AgencyManager;
import de.infinitehorizon.data.BalanceManager;
import de.infinitehorizon.research.FpLedger;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@code field_scanner} (11.1, 18): Rechtsklick auf eine Kreatur aus {@code analyzable} gibt der Agentur einmal je
 * Kreaturentyp {@code creature_analysis.fp} FP. Gezählt wird im Probenzähler unter {@code creature:<id>}.
 */
public class FieldScannerItem extends Item {

    public static final TagKey<EntityType<?>> ANALYZABLE = TagKey.create(Registries.ENTITY_TYPE, InfiniteHorizon.id("analyzable"));

    public FieldScannerItem(Properties properties) {
        super(properties);
    }

    /** Analyse für die Agentur des Spielers; FP in Hundertsteln oder 0 (schon analysiert, nicht analysierbar). */
    public static int analyze(ServerPlayer player, LivingEntity target) {
        if (!target.getType().builtInRegistryHolder().is(ANALYZABLE)) {
            return 0;
        }
        Agency agency = AgencyManager.ensureFor(player);
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        String key = "creature:" + type;
        if (agency.progress().sampleCount(key) > 0) {
            return 0;
        }
        JsonObject rules = BalanceManager.get().entry("research", "creature_analysis").orElseGet(JsonObject::new);
        // FP in Hundertsteln (11.1)
        int value = (int) Math.round((rules.has("fp") ? rules.get("fp").getAsDouble() : 0) * 100);
        agency.progress().countSample(key);
        agency.progress().addFp(value);
        AgencyData.get(player.level().getServer()).setDirty();
        ResearchService.syncAgency(player.level().getServer(), agency);
        return value;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            int value = analyze(sp, target);
            sp.sendOverlayMessage(value > 0
                    ? Component.translatable("message." + InfiniteHorizon.MOD_ID + ".field_scanner.done", target.getName(), FpLedger.format(value))
                    : Component.translatable("message." + InfiniteHorizon.MOD_ID + ".field_scanner.known", target.getName()));
        }
        return InteractionResult.SUCCESS;
    }
}
