package de.infinitehorizon.gametest;

import de.infinitehorizon.InfiniteHorizon;
import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyAdvancements;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.Role;
import de.infinitehorizon.client.AmbientSounds;
import de.infinitehorizon.client.ResearchTerminalScreen;
import de.infinitehorizon.client.SoundDamping;
import de.infinitehorizon.client.handbook.HandbookClient;
import de.infinitehorizon.client.handbook.HandbookScreen;
import de.infinitehorizon.logistics.CargoFlights;
import de.infinitehorizon.logistics.CargoRockets;
import de.infinitehorizon.logistics.CargoTerminalBlockEntity;
import de.infinitehorizon.logistics.TradingPostBlockEntity;
import de.infinitehorizon.machine.MachineBlock;
import de.infinitehorizon.machine.ProcessingMachineBlockEntity;
import de.infinitehorizon.registry.ModBlocks;
import de.infinitehorizon.registry.ModItems;
import de.infinitehorizon.research.ResearchService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

/**
 * Spieltest M9 im echten Client (Abnahme 9g): Agentur-Reiter mit echten Klicks (Reiter, Rolle, Umbenennen),
 * Partnerschaft annehmen, Freigabe am Frachtterminal, Tausch am Handelsposten einer Partner-Agentur und ein Frachtflug
 * dorthin, die neuen Erfolge auf Mars und Venus, Klang im Vakuum und an einer laufenden Maschine, das englische
 * Handbuch mit der Mars-Seite nach dem Besuch.
 */
public class IhClientPlaytestM9 extends IhPlaytestBase {

    private UUID member;
    private UUID partnerLeader;
    private UUID partnerAgency;
    private BlockPos terminal;
    private BlockPos post;
    private BlockPos research;

    @Override
    protected String name() {
        return "playtest_m9";
    }

    @Override
    protected void steps() {
        step("Agentur: Reiter, Rolle, Name", this::agency);
        step("Partnerschaft annehmen", this::partners);
        step("Freigabe, Handelsposten, Fracht zum Partner", this::trade);
        step("Erfolge auf Mars und Venus", this::advancements);
        step("Klang im Vakuum und an der Maschine", this::sound);
        step("Handbuch: Mars-Seite nach dem Besuch", this::handbook);
    }

    private BlockPos place(Block block, int east) {
        return server(p -> {
            BlockPos at = p.blockPosition().east(east);
            var state = block.defaultBlockState();
            if (state.hasProperty(MachineBlock.FACING)) {
                state = state.setValue(MachineBlock.FACING, Direction.WEST);
            }
            p.level().setBlockAndUpdate(at, state);
            return at;
        });
    }

    private void open(BlockPos pos, Class<? extends net.minecraft.client.gui.screens.Screen> screen) {
        context.waitTicks(5);
        context.getInput().lookAt(pos);
        context.waitTick();
        context.getInput().pressKey(o -> o.keyUse);
        context.waitForScreen(screen);
        context.waitTicks(5);
    }

    private void tab(int t) {
        int[] c = context.computeOnClient(cl -> ((ResearchTerminalScreen) cl.gui.screen()).tabCenter(t));
        clickAt(c[0], c[1]);
        context.waitTicks(3);
        int now = context.computeOnClient(cl -> ((ResearchTerminalScreen) cl.gui.screen()).tab());
        check(now == t, "Reiter " + t + " reagiert nicht auf den Klick (offen: " + now + ")");
    }

    /** Reiter „Agentur“ per Klick, Rolle eines Mitglieds wechseln, Agentur umbenennen. */
    private void agency() {
        member = UUID.randomUUID();
        sp.getServer().runOnServer(s -> {
            AgencyData d = AgencyData.get(s);
            ServerPlayer p = player(s);
            Agency own = d.ensureFor(p.getUUID(), s.overworld().getGameTime());
            d.ensureFor(member, s.overworld().getGameTime());
            d.invite(p.getUUID(), member);
            d.accept(member, own.id(), s.overworld().getGameTime());
            ResearchService.sendState(p);
        });
        research = place(ModBlocks.RESEARCH_TERMINAL.get(), 2);
        open(research, ResearchTerminalScreen.class);
        tab(ResearchTerminalScreen.TAB_AGENCY);
        check(click(Component.translatable("gui.infinite_horizon.agency.role.engineer")), "Knopf „Ingenieur“ fehlt");
        context.waitTicks(10);
        Role role = server(p -> AgencyData.get(p.level().getServer()).agencyOf(p.getUUID())
                .flatMap(a -> a.roleOf(member)).orElse(null));
        check(role == Role.GUEST, "Rolle nach dem Klick: " + role);
        // Umbenennen: Textfeld anklicken, tippen, Knopf
        int[] box = context.computeOnClient(c -> {
            for (var e : c.gui.screen().children()) {
                if (e instanceof net.minecraft.client.gui.components.EditBox b) {
                    return new int[]{b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2};
                }
            }
            return null;
        });
        check(box != null, "Textfeld für den Namen fehlt");
        if (box != null) {
            clickAt(box[0], box[1]);
            context.runOnClient(c -> {
                for (var e : c.gui.screen().children()) {
                    if (e instanceof net.minecraft.client.gui.components.EditBox b) {
                        b.setValue("");
                    }
                }
            });
            context.getInput().typeChars("Saturn Express");
            check(click(Component.translatable("gui.infinite_horizon.agency.rename")), "Knopf „Umbenennen“ fehlt");
            context.waitTicks(10);
            String name = server(p -> AgencyData.get(p.level().getServer()).agencyOf(p.getUUID()).map(Agency::name).orElse(null));
            check("Saturn Express".equals(name), "Name nach dem Umbenennen: " + name);
        }
        shot("play9_agency");
    }

    /** Eine andere Agentur bietet an, der Spieler nimmt im Reiter „Partner“ an. */
    private void partners() {
        partnerLeader = UUID.randomUUID();
        sp.getServer().runOnServer(s -> {
            AgencyData d = AgencyData.get(s);
            Agency other = d.ensureFor(partnerLeader, s.overworld().getGameTime());
            other.setName("Ringwerft Titan");
            partnerAgency = other.id();
            d.requestPartner(partnerLeader, d.agencyOf(player(s).getUUID()).orElseThrow().id());
            ResearchService.sendState(player(s));
        });
        open(research, ResearchTerminalScreen.class); // jeder Schritt beginnt ohne offenen Bildschirm
        tab(ResearchTerminalScreen.TAB_PARTNERS);
        check(click(Component.translatable("gui.infinite_horizon.agency.accept")), "Knopf „Annehmen“ fehlt");
        context.waitTicks(10);
        boolean partners = server(p -> {
            AgencyData d = AgencyData.get(p.level().getServer());
            return d.arePartners(d.agencyOf(p.getUUID()).orElseThrow().id(), partnerAgency);
        });
        check(partners, "keine Partnerschaft nach „Annehmen“");
        shot("play9_partners");
        context.setScreen(() -> null);
    }

    /** Freigabe per Knopf, Tausch als Fremder am Handelsposten des Partners, Frachtflug dorthin. */
    private void trade() {
        go("mars");
        terminal = place(ModBlocks.CARGO_TERMINAL.get(), 3);
        post = place(ModBlocks.TRADING_POST.get(), -3);
        sp.getServer().runOnServer(s -> {
            ServerPlayer p = player(s);
            if (p.level().getBlockEntity(post) instanceof TradingPostBlockEntity t) {
                t.setOwner(partnerAgency);
                t.setOffer(0, new ItemStack(Items.IRON_INGOT, 4));
                t.setOffer(1, new ItemStack(Items.EMERALD, 1));
                t.insertItem(new ItemStack(Items.EMERALD, 3), false);
            }
            p.getInventory().add(new ItemStack(Items.IRON_INGOT, 8));
        });
        // eigenes Terminal: öffnen (wird Besitz der Agentur), Freigabe per Knopf
        open(terminal, de.infinitehorizon.client.CargoTerminalScreen.class);
        check(click(Component.translatable("gui.infinite_horizon.cargo_terminal.partners.off")), "Knopf „Partner: aus“ fehlt");
        context.waitTicks(5);
        boolean shared = server(p -> p.level().getBlockEntity(terminal) instanceof CargoTerminalBlockEntity t && t.shared());
        check(shared, "Terminal nach dem Klick nicht freigegeben");
        shot("play9_cargo_terminal_shared");
        context.setScreen(() -> null);
        // Handelsposten des Partners: Tausch per Knopf. Der Weltbesitzer im Einzelspieler bleibt Operator und darf alles
        // (23.1); für Nicht-Mitglieder gilt: kein Zugriff auf das Lager, Tauschen schon.
        open(post, de.infinitehorizon.client.TradingPostScreen.class);
        boolean guestSlots = de.infinitehorizon.agency.Permissions.allowed(null, de.infinitehorizon.agency.Permissions.Action.USE_SLOTS);
        boolean member = server(p -> AgencyData.get(p.level().getServer()).byId(partnerAgency)
                .map(a -> a.isMember(p.getUUID())).orElse(true));
        check(!guestSlots && !member, "Lager des Partner-Postens: Gast-Slots " + guestSlots + ", Mitglied " + member);
        check(click(Component.translatable("gui.infinite_horizon.trading_post.trade")), "Knopf „Tauschen“ fehlt");
        context.waitTicks(10);
        int emeralds = context.computeOnClient(c -> c.player.getInventory().countItem(Items.EMERALD));
        check(emeralds == 1, "Smaragde nach dem Tausch: " + emeralds);
        shot("play9_trading_post");
        context.setScreen(() -> null);
        // Frachtflug vom eigenen Terminal zum Handelsposten des Partners
        context.waitTicks(25);
        int delivered = server(p -> {
            ServerLevel l = (ServerLevel) p.level();
            if (!(l.getBlockEntity(terminal) instanceof CargoTerminalBlockEntity t)
                    || !(l.getBlockEntity(post) instanceof TradingPostBlockEntity tp)) {
                return -1;
            }
            ItemStack rocket = new ItemStack(ModItems.CARGO_ROCKET.get());
            CargoRockets.setFuel(rocket, CargoRockets.fuelTank(rocket).get("capacity").getAsLong());
            t.setItem(t.layout().moldStart(), rocket);
            t.insertItem(new ItemStack(Items.COPPER_INGOT, 16), false);
            var r = CargoFlights.launch(l, t, 0, l.dimension().identifier(), post, s -> true, f -> true);
            if (r != CargoFlights.Launch.OK) {
                return -2;
            }
            CargoFlights data = CargoFlights.get(l.getServer());
            var f = data.flightOf(l.dimension().identifier(), terminal, 0).orElseThrow();
            data.arriveNow(f.id(), l.getServer().overworld().getGameTime());
            CargoFlights.process(l.getServer());
            return (int) tp.amountOf("minecraft:copper_ingot");
        });
        check(delivered == 16, "Fracht am Handelsposten des Partners: " + delivered);
    }

    private boolean done(String id) {
        return server(p -> {
            var holder = p.level().getServer().getAdvancements().get(InfiniteHorizon.id(id));
            return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
        });
    }

    /** Rote Erde (Mars betreten, im vorigen Schritt) und Drei Minuten Hölle (Venus unter die Wolken und zurück). */
    private void advancements() {
        check(done("red_earth"), "Erfolg „Rote Erde“ fehlt nach dem Mars-Besuch");
        go("venus");
        double x = context.computeOnClient(c -> c.player.getX());
        double z = context.computeOnClient(c -> c.player.getZ());
        tpHere(x, 150, z, 0, 0);
        context.waitTicks(30);
        tpHere(x, 205, z, 0, 0);
        context.waitTicks(30);
        check(done("three_minutes_of_hell"), "Erfolg „Drei Minuten Hölle“ fehlt nach Abstieg und Rückkehr");
        context.runOnClient(c -> {
            var adv = c.player.connection.getAdvancements();
            var root = adv.get(InfiniteHorizon.id("root"));
            if (root != null) {
                adv.setSelectedTab(root, true);
            }
        });
        context.setScreen(() -> new net.minecraft.client.gui.screens.advancements.AdvancementsScreen(
                net.minecraft.client.Minecraft.getInstance().player.connection.getAdvancements()));
        context.waitTicks(10);
        shot("play9_advancements");
        context.setScreen(() -> null);
    }

    /** Mond: Vakuum erkannt, Atem- und Funkschleife; ein laufender Zerkleinerer spielt sein Geräusch. */
    private void sound() {
        go("moon");
        context.waitTicks(40);
        boolean vacuum = context.computeOnClient(c -> SoundDamping.vacuum());
        boolean loops = context.computeOnClient(c -> AmbientSounds.suitLoopsRequested());
        check(vacuum && loops, "Klang im Vakuum: Vakuum " + vacuum + ", Anzug-Schleifen " + loops);
        BlockPos crusher = place(ModBlocks.CRUSHER.get(), 2);
        for (int i = 0; i < 6; i++) {
            sp.getServer().runOnServer(s -> {
                if (player(s).level().getBlockEntity(crusher) instanceof ProcessingMachineBlockEntity m) {
                    m.refreshLayout();
                    m.energyStorage().setAmountUnchecked(m.energyStorage().getCapacity());
                    if (m.getItem(0).isEmpty()) {
                        m.setItem(0, new ItemStack(Items.RAW_IRON, 16));
                    }
                }
            });
            context.waitTicks(10);
        }
        int machine = context.computeOnClient(c -> AmbientSounds.machineLoops());
        check(machine > 0, "kein Maschinengeräusch am laufenden Zerkleinerer");
    }

    /** Englisches Handbuch: Die Mars-Seite zeigt nach dem Besuch den Atlas-Kasten und den ganzen Text. */
    private void handbook() {
        context.runOnClient(c -> HandbookClient.open("infinite_horizon:phase_3/mars"));
        context.waitForScreen(HandbookScreen.class);
        context.waitTicks(10);
        String page = context.computeOnClient(c -> ((HandbookScreen) c.gui.screen()).currentPage());
        check("infinite_horizon:phase_3/mars".equals(page), "Mars-Seite nicht offen: " + page);
        boolean visited = context.computeOnClient(c -> HandbookScreen.PlanetAtlas.of("infinite_horizon:mars").visited());
        check(visited, "Atlas: Mars nicht als besucht bekannt");
        shot("play9_handbook_mars");
        context.setScreen(() -> null);
    }
}
