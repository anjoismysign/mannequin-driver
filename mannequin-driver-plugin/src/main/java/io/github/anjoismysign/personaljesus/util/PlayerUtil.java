package io.github.anjoismysign.personaljesus.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PlayerUtil {

    public static boolean isPlayerGrounded(Player player) {
        Location loc = player.getLocation();
        Block below = loc.clone().subtract(0, 1, 0).getBlock();
        return below.getType().isSolid();
    }

}
