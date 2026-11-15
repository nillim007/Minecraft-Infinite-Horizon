package de.infinitehorizon.creature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/**
 * {@code drift_mite} (18): harmlos, schwebt in Schwärmen um Platinerz der Asteroiden. Entsteht nur mit der Welt
 * (Feature {@code asteroid}), spawnt nicht nach.
 */
public class DriftMite extends PlanetCreature {

    private static final int TURN = 10;

    public DriftMite(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, TURN, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
