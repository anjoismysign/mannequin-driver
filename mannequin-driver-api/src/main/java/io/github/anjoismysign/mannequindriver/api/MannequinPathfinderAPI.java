package io.github.anjoismysign.mannequindriver.api;

import io.github.anjoismysign.mannequindriver.manager.MannequinNavigator;
import io.github.anjoismysign.mannequindriver.manager.NavigationManager;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * Main entry point API class for the MannequinPathfinder library.
 *
 * <p>This class provides static factory methods for creating NavigationManager instances
 * and serves as the primary interface for plugin integration.</p>
 *
 * <h2>Quick Start</h2>
 *
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     private NavigationManager navigationManager;
 *
 *     @Override
 *     public void onEnable() {
 *         // Initialize
 *         navigationManager = MannequinPathfinderAPI.createManager(this);
 *
 *     public void navigateMannequin(LivingEntity mannequin, Location target) {
 *         MannequinNavigator navigator = MannequinPathfinderAPI.register(navigationManager, mannequin);
 *         navigator.moveTo(target, 1.0);
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         MannequinPathfinderAPI.cleanupAll(navigationManager);
 *     }
 * }
 * }
 */
public final class MannequinPathfinderAPI {

    /**
     * Creates a new NavigationManager
     *
     * @param plugin The owning plugin instance
     * @return A new NavigationManager instance
     * @throws IllegalArgumentException if plugin is null
     */
    @NotNull
    public static NavigationManager createManager(@NotNull JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        return new NavigationManager(plugin);
    }

    /**
     * Registers a Mannequin for pathfinding navigation using the manager's defaults.
     *
     * <p>Equivalent to:</p>
     * <pre>{@code
     * manager.register(mannequin)
     * }</pre>
     *
     * @param manager The NavigationManager to use
     * @param mannequin The Mannequin entity to register
     * @return A MannequinNavigator for controlling movement
     * @throws IllegalArgumentException if manager or mannequin is null
     */
    @NotNull
    public static MannequinNavigator register(
            @NotNull NavigationManager manager,
            @NotNull Mannequin mannequin
    ) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        Objects.requireNonNull(mannequin, "Mannequin cannot be null");
        return manager.register(mannequin);
    }

    /**
     * Gets the navigator for a registered Mannequin.
     *
     * @param manager The NavigationManager to query
     * @param mannequinId The UUID of the Mannequin
     * @return The MannequinNavigator, or null if not registered
     */
    @Nullable
    public static MannequinNavigator getNavigator(
            @NotNull NavigationManager manager,
            @NotNull UUID mannequinId
    ) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        Objects.requireNonNull(mannequinId, "Mannequin ID cannot be null");
        return manager.getNavigator(mannequinId);
    }

    // ==================== Cleanup Convenience Methods ====================

    /**
     * Cleans up all navigators and drivers managed by the NavigationManager.
     *
     * <p>This should be called during plugin disable to ensure proper cleanup
     * of all driver entities.</p>
     *
     * @param manager The NavigationManager to clean up
     */
    public static void cleanupAll(@NotNull NavigationManager manager) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        manager.cleanupAll();
    }

    /**
     * Unregisters a Mannequin and removes its driver.
     *
     * @param manager The NavigationManager
     * @param mannequinId The Mannequin uniqueId to unregister
     * @return true if the Mannequin was found and unregistered
     */
    public static boolean unregister(
            @NotNull NavigationManager manager,
            @NotNull UUID mannequinId
    ) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        Objects.requireNonNull(mannequinId, "MannequinId cannot be null");
        return manager.unregister(mannequinId);
    }

    /**
     * Gets the count of registered navigators.
     *
     * @param manager The NavigationManager to query
     * @return The number of active navigators
     */
    public static int getRegisteredCount(@NotNull NavigationManager manager) {
        Objects.requireNonNull(manager, "Manager cannot be null");
        return manager.getRegisteredCount();
    }
}
