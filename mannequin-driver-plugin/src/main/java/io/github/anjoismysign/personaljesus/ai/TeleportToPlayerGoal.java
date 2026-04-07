package io.github.anjoismysign.personaljesus.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import io.github.anjoismysign.mannequindriver.manager.MannequinNavigator;
import io.github.anjoismysign.personaljesus.PersonalJesus;
import io.github.anjoismysign.personaljesus.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

public class TeleportToPlayerGoal implements Goal<@NotNull Mob> {

    private final MannequinNavigator navigator;
    private final UUID targetUUID;
    private Player target;

    private static final double MAX_DISTANCE_SQUARED = 20 * 20;

    public TeleportToPlayerGoal(MannequinNavigator navigator, UUID targetUUID) {
        this.navigator = navigator;
        this.targetUUID = targetUUID;
    }

    @Override
    public @NotNull GoalKey<@NotNull Mob> getKey() {
        return GoalKey.of(Mob.class, new NamespacedKey(PersonalJesus.getInstance(), "teleport_to_player"));
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }

    @Override
    public boolean shouldActivate() {
        target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            return false;
        }

        if (!PlayerUtil.isPlayerGrounded(target)) {
            return false;
        }

        var mob = navigator.getDriver();

        if (!mob.getWorld().getName().equals(target.getWorld().getName())) {
            return false;
        }

        double distanceSquared = mob.getLocation().distanceSquared(target.getLocation());
        return distanceSquared > MAX_DISTANCE_SQUARED;
    }

    @Override
    public void start() {
        if (target == null) {
            return;
        }
        Location base = target.getLocation();
        @Nullable Location safe = findSafeLocation(base);
        if (safe == null) {
            return;
        }
        navigator.teleport(safe);
    }

    @Override
    public boolean shouldStayActive() {
        return false;
    }

    @Override
    public void stop(){
        target = null;
    }

    private Location findSafeLocation(Location center) {
        World world = center.getWorld();
        int baseX = center.getBlockX();
        int baseY = center.getBlockY();
        int baseZ = center.getBlockZ();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) {
                    continue;
                }

                int checkX = baseX + x;
                int checkZ = baseZ + z;

                Block ground = world.getBlockAt(checkX, baseY - 1, checkZ);
                Block feet = world.getBlockAt(checkX, baseY, checkZ);
                Block head = world.getBlockAt(checkX, baseY + 1, checkZ);

                if (isSafe(ground, feet, head)) {
                    return new Location(world, checkX + 0.5, baseY, checkZ + 0.5);
                }
            }
        }

        return null;
    }

    private boolean isSafe(Block ground, Block feet, Block head) {
        return ground.getType().isSolid()
                && feet.isPassable()
                && head.isPassable();
    }
}