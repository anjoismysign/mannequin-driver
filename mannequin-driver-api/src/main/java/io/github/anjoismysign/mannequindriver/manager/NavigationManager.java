package io.github.anjoismysign.mannequindriver.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>Example usage:</p>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     private NavigationManager navigationManager;
 *
 *     @Override
 *     public void onEnable() {
 *         navigationManager = new NavigationManager(this);
 *     }
 *
 *     public void setupNavigation(LivingEntity mannequin) {
 *         MannequinNavigator nav = navigationManager.register(mannequin);
 *         nav.moveTo(targetLocation, 1.5);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         navigationManager.cleanupAll();
 *     }
 * }
 * }</pre>
 */
public final class NavigationManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, MannequinNavigator> navigators;
    private final Map<UUID, UUID> driverToMannequin;

    private boolean autoCleanupOnDeath;
    private boolean respawnDriverOnDisconnect;
    private final BukkitTask task;

    /**
     * Creates a new NavigationManager.
     *
     * @param plugin The owning JavaPlugin instance
     * @throws IllegalArgumentException if plugin or driverType is null
     */
    public NavigationManager(@NotNull JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        this.plugin = plugin;
        this.navigators = new HashMap<>();
        this.driverToMannequin = new HashMap<>();
        this.autoCleanupOnDeath = true;
        this.respawnDriverOnDisconnect = true;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                navigators.values().forEach(MannequinNavigator::sync);
            }
        }.runTaskTimer(plugin, 0, 1);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Registers a Mannequin entity with custom driver type and attributes.
     *
     * <p>This is the full registration method that provides complete control over
     * the driver entity's appearance and behavior.</p>
     *
     * @param mannequin The Mannequin entity to register
     * @return A MannequinNavigator for controlling movement
     * @throws IllegalArgumentException if any parameter is null
     * @throws IllegalStateException    if the Mannequin is already registered with a different navigator
     */
    @NotNull
    public MannequinNavigator register(@NotNull Mannequin mannequin) {
        Objects.requireNonNull(mannequin, "Mannequin cannot be null");

        UUID mannequinId = mannequin.getUniqueId();

        if (navigators.containsKey(mannequinId)) {
            return navigators.get(mannequinId);
        }

        Location mannequinLocation = mannequin.getLocation();
        Zombie driver = (Zombie) mannequinLocation.getWorld().spawnEntity(mannequinLocation, EntityType.ZOMBIE);
        driver.setPersistent(false);
        driver.setCollidable(false);
        driver.setSilent(true);
        driver.setVisibleByDefault(false);
        driver.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false
        ));
        driver.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false
        ));

        Bukkit.getMobGoals().removeAllGoals(driver);
        MannequinNavigator navigator = new MannequinNavigator(mannequin, driver, this);

        navigators.put(mannequinId, navigator);
        driverToMannequin.put(driver.getUniqueId(), mannequinId);

        return navigator;
    }

    /**
     * Unregisters a Mannequin and removes its associated Driver.
     *
     * @param mannequinId The UUID of the Mannequin to unregister
     * @return true if the Mannequin was found and unregistered, false otherwise
     */
    public boolean unregister(@NotNull UUID mannequinId) {
        MannequinNavigator navigator = navigators.remove(mannequinId);
        if (navigator != null) {
            driverToMannequin.remove(navigator.getDriverId());
            return true;
        }
        return false;
    }

    /**
     * Gets the navigator for a registered Mannequin.
     *
     * @param mannequinId The UUID of the Mannequin
     * @return The MannequinNavigator, or null if not registered
     */
    @Nullable
    public MannequinNavigator getNavigator(@NotNull UUID mannequinId) {
        return navigators.get(mannequinId);
    }

    /**
     * Gets the navigator for a registered driver.
     *
     * @param driverId The UUID of the driver.
     * @return The MannequinNavigator, or null if not registered.
     */
    @Nullable
    public MannequinNavigator getNavigatorByDriverId(@NotNull UUID driverId) {
        UUID mannequinId = driverToMannequin.get(driverId);
        return mannequinId == null ? null : getNavigator(mannequinId);
    }

    /**
     * Gets the navigator for a Mannequin entity.
     *
     * @param mannequin The Mannequin entity
     * @return The MannequinNavigator, or null if not registered
     */
    @Nullable
    public MannequinNavigator getNavigator(@NotNull LivingEntity mannequin) {
        return navigators.get(mannequin.getUniqueId());
    }

    /**
     * Checks if a Mannequin is registered for pathfinding.
     *
     * @param mannequinId The UUID of the Mannequin
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(@NotNull UUID mannequinId) {
        return navigators.containsKey(mannequinId);
    }

    /**
     * Checks if a Mannequin entity is registered.
     *
     * @param mannequin The Mannequin entity
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(@NotNull LivingEntity mannequin) {
        return isRegistered(mannequin.getUniqueId());
    }

    /**
     * Gets the number of registered Mannequins.
     *
     * @return The count of active navigators
     */
    public int getRegisteredCount() {
        return navigators.size();
    }

    /**
     * Gets an unmodifiable collection of all registered navigators.
     *
     * @return Collection of MannequinNavigators
     */
    @NotNull
    public Collection<MannequinNavigator> getAllNavigators() {
        return Collections.unmodifiableCollection(navigators.values());
    }

    /**
     * Removes all navigators and their associated Drivers.
     *
     * <p>This should be called during plugin disable to ensure proper cleanup.</p>
     */
    public void cleanupAll() {
        for (MannequinNavigator navigator : new ArrayList<>(navigators.values())) {
            navigator.remove();
        }
        navigators.clear();
        driverToMannequin.clear();
    }

    /**
     * Sets whether to automatically clean up drivers when Mannequins die.
     *
     * @param autoCleanup true for automatic cleanup (default: true)
     */
    public void setAutoCleanupOnDeath(boolean autoCleanup) {
        this.autoCleanupOnDeath = autoCleanup;
    }

    /**
     * Gets whether auto cleanup on death is enabled.
     *
     * @return true if auto cleanup is enabled
     */
    public boolean isAutoCleanupOnDeath() {
        return autoCleanupOnDeath;
    }

    /**
     * Sets whether to respawn the driver if the mannequin becomes disconnected.
     *
     * @param respawn true to respawn driver on disconnect (default: true)
     */
    public void setRespawnDriverOnDisconnect(boolean respawn) {
        this.respawnDriverOnDisconnect = respawn;
    }

    /**
     * Gets whether driver respawn on disconnect is enabled.
     *
     * @return true if respawn is enabled
     */
    public boolean isRespawnDriverOnDisconnect() {
        return respawnDriverOnDisconnect;
    }

    @EventHandler
    public void onMannequinRemove(EntityRemoveEvent event) {
        Entity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();

        @Nullable MannequinNavigator navigator = navigators.remove(entityId);
        if (navigator != null && autoCleanupOnDeath && !navigator.isStopped()) {
            navigator.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDriverDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        @Nullable MannequinNavigator navigator = getNavigatorByDriverId(entity.getUniqueId());
        if (navigator == null) {
            return;
        }
        Zombie driver = navigator.getDriver();
        if (driver.isValid() && driver.getUniqueId().equals(entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            cleanupAll();
        }
    }

    /**
     * Gets the owning plugin instance.
     *
     * @return The JavaPlugin instance
     */
    @NotNull
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
