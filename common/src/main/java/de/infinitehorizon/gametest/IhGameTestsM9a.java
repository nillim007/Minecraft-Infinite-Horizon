package de.infinitehorizon.gametest;

import de.infinitehorizon.agency.Agency;
import de.infinitehorizon.agency.AgencyData;
import de.infinitehorizon.agency.Permissions;
import de.infinitehorizon.agency.Role;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** GameTests für M9a (23.1): Rollenwechsel wirkt auf die Rechte, Leitung übergeben. */
public final class IhGameTestsM9a {

    private IhGameTestsM9a() {
    }

    /** Ein Mitglied mit der Rolle Gast darf an Blöcken der eigenen Agentur nur öffnen. */
    public static void guestRole(GameTestHelper h) {
        ServerPlayer p = IhTestPlayers.inList(h, GameType.SURVIVAL, new Vec3(1.5, 2, 1.5));
        try {
            AgencyData data = AgencyData.get(h.getLevel().getServer());
            long tick = h.getLevel().getGameTime();
            UUID leader = UUID.randomUUID();
            Agency a = data.ensureFor(leader, tick);
            data.ensureFor(p.getUUID(), tick);
            h.assertTrue(data.invite(leader, p.getUUID()) == AgencyData.ActionResult.OK, "Einladung");
            h.assertTrue(data.accept(p.getUUID(), a.id(), tick) == AgencyData.ActionResult.OK, "Beitritt");
            h.assertTrue(Permissions.can(p, a.id(), Permissions.Action.BREAK), "Ingenieur darf nicht abbauen");
            h.assertTrue(data.setRole(leader, p.getUUID(), Role.GUEST) == AgencyData.ActionResult.OK, "Rolle Gast");
            h.assertTrue(Permissions.can(p, a.id(), Permissions.Action.OPEN), "Gast darf nicht öffnen");
            h.assertFalse(Permissions.can(p, a.id(), Permissions.Action.BREAK), "Gast darf abbauen");
            h.assertFalse(Permissions.can(p, a.id(), Permissions.Action.USE_SLOTS), "Gast darf Slots benutzen");
            h.assertTrue(data.transferLeader(leader, p.getUUID()) == AgencyData.ActionResult.OK, "Leitung übergeben");
            h.assertTrue(a.roleOf(p.getUUID()).orElse(null) == Role.LEADER, "neuer Leiter");
            h.assertTrue(Permissions.can(p, a.id(), Permissions.Action.BREAK), "Leiter darf nicht abbauen");
        } finally {
            IhTestPlayers.remove(p);
        }
        h.succeed();
    }
}
