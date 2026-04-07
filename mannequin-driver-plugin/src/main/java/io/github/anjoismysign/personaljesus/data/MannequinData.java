package io.github.anjoismysign.personaljesus.data;

import io.github.anjoismysign.mannequindriver.manager.MannequinNavigator;
import io.github.anjoismysign.personaljesus.PersonalJesus;
import io.github.anjoismysign.personaljesus.ai.FollowPlayerGoal;
import io.github.anjoismysign.personaljesus.ai.TeleportToPlayerGoal;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Zombie;

import java.util.UUID;

public record MannequinData(Mannequin mannequin, MannequinNavigator navigator) {

    public static MannequinData of(UUID playerId,
                                   Mannequin mannequin,
                                   PersonalJesus plugin){
        MannequinNavigator navigator = plugin.getNavigationManager().register(mannequin);
        Zombie driver = navigator.getDriver();
        Bukkit.getMobGoals().addGoal(driver, 0, new TeleportToPlayerGoal(navigator, playerId));
        Bukkit.getMobGoals().addGoal(driver, 1, new FollowPlayerGoal(navigator, playerId));
        return new MannequinData(mannequin,navigator);
    }
}
