package io.github.anjoismysign.personaljesus;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.anjoismysign.mannequindriver.api.MannequinPathfinderAPI;
import io.github.anjoismysign.mannequindriver.manager.NavigationManager;
import io.github.anjoismysign.personaljesus.data.MannequinData;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PersonalJesus extends JavaPlugin implements Listener {
    private static PersonalJesus instance;

    public static PersonalJesus getInstance(){
        return instance;
    }

    private Map<UUID, MannequinData> data;
    private NavigationManager navigationManager;

    @Override
    public void onEnable(){
        instance = this;
        data = new HashMap<>();
        navigationManager = MannequinPathfinderAPI.createManager(this);
        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getOnlinePlayers().forEach(this::load);
    }

    @Override
    public void onDisable(){
        navigationManager.cleanupAll();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        load(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        @Nullable MannequinData personalJesus = data.get(playerId);
        if (personalJesus == null){
            return;
        }
        personalJesus.navigator().remove();
        data.remove(playerId);
    }

    public NavigationManager getNavigationManager(){
        return navigationManager;
    }

    private ResolvableProfile resolvableProfile(@NotNull String url,
                                                @NotNull PlayerTextures.SkinModel model) {
        var json = "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}".formatted(url);
        var base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return ResolvableProfile.resolvableProfile()
                .addProperty(new ProfileProperty("textures", base64))
                .skinPatch(builder->{
                    builder.model(model);
                })
                .build();
    }

    private void load(@NotNull Player player){
        UUID playerId = player.getUniqueId();
        Mannequin mannequin = (Mannequin) player.getWorld().spawnEntity(player.getLocation(), EntityType.MANNEQUIN);
        mannequin.setProfile(resolvableProfile("https://textures.minecraft.net/texture/b119bf3b2a356a158469c70aa0db3ab4b8f302b1d19f27a7bb6ed9c56945a1", PlayerTextures.SkinModel.SLIM));
        mannequin.setInvulnerable(true);
        mannequin.setPersistent(false);
        mannequin.setDescription(null);
        mannequin.customName(null);
        data.put(playerId, MannequinData.of(playerId, mannequin, this));
    }

}
