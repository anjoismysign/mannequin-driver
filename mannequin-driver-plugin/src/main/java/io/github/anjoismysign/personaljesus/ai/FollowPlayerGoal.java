package io.github.anjoismysign.personaljesus.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import io.github.anjoismysign.mannequindriver.manager.MannequinNavigator;
import io.github.anjoismysign.personaljesus.PersonalJesus;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.UUID;

public class FollowPlayerGoal implements Goal<@NotNull Mob> {

    private final MannequinNavigator navigator;
    private final UUID targetUUID;
    private Player target;

    private static final double MIN_DISTANCE_SQUARED = 3 * 3;
    private static final double MAX_DISTANCE_SQUARED = 20 * 20;

    public FollowPlayerGoal(MannequinNavigator navigator, UUID targetUUID) {
        this.navigator = navigator;
        this.targetUUID = targetUUID;
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GoalKey.of(Mob.class, new NamespacedKey(PersonalJesus.getInstance(), "follow_player"));
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }

    @Override
    public boolean shouldActivate() {
        target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            return false;
        }

        var mob = navigator.getDriver();

        double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());
        return distanceSquared >= MIN_DISTANCE_SQUARED && distanceSquared <= MAX_DISTANCE_SQUARED;
    }

    @Override
    public boolean shouldStayActive() {
        return shouldActivate();
    }

    @Override
    public void stop() {
        target = null;
        var mob = navigator.getDriver();
        mob.getPathfinder().stopPathfinding();
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        var mob = navigator.getDriver();
        mob.lookAt(target.getLocation());
        navigator.moveTo(target.getLocation(), 1.0);
    }
}