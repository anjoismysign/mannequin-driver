package io.github.anjoismysign.mannequindriver.manager;

import com.destroystokyo.paper.entity.Pathfinder;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * <p>Example usage:</p>
 * <pre>{@code
 * Mannequin mannequin = (Mannequin) entity;
 * MannequinNavigator navigator = NavigationManager.register(mannequin);
 *
 * if (navigator.moveTo(targetLocation, true)) {
 *     System.out.println("Pathfinding started successfully!");
 * }
 * }</pre>
 */
public final class MannequinNavigator {

    private final UUID mannequinId;
    private final UUID driverId;
    private final Mannequin mannequin;
    private final Zombie driver;
    private final NavigationManager manager;

    private boolean isNavigating;
    private boolean isStopped;

    private Location lastTarget;
    private double lastSpeed;

    @ApiStatus.Internal
    MannequinNavigator(
            @NotNull Mannequin mannequin,
            @NotNull Zombie driver,
            @NotNull NavigationManager manager
    ) {
        this.mannequinId = mannequin.getUniqueId();
        this.driverId = driver.getUniqueId();
        this.mannequin = mannequin;
        this.driver = driver;
        this.manager = manager;
        this.isNavigating = false;
        this.isStopped = false;
    }

    /**
     * Attempts to move the Mannequin to the specified target location using pathfinding.
     * @param target The target location to navigate to
     * @param speed The movement speed multiplier (1.0 = walking, 1.3 = sprinting)
     * @return true if pathfinding calculation succeeded and movement has started,
     *         false if pathfinding failed (e.g., no valid path exists, target unreachable)
     * @throws IllegalStateException if this navigator has been stopped
     * @throws IllegalArgumentException if target is null or in a different world than the Mannequin
     */
    public boolean moveTo(@NotNull Location target, double speed) {
        checkNotStopped();

        if (target == null) {
            throw new NullPointerException("Target location cannot be null");
        }

        World mannequinWorld = mannequin.getWorld();
        if (!target.getWorld().getName().equals(mannequinWorld.getName())) {
            throw new IllegalArgumentException(
                    "Target world (" + target.getWorld().getName() +
                    ") does not match Mannequin world (" + mannequinWorld.getName() + ")"
            );
        }

        this.lastTarget = target.clone();
        this.lastSpeed = Math.max(0.1, speed);

        Pathfinder pathfinder = driver.getPathfinder();

        boolean successful = pathfinder.moveTo(target, speed);
        isNavigating = successful;

        return successful;
    }

    /**
     * Checks if the Mannequin is currently navigating toward a target.
     *
     * @return true if navigation is in progress, false otherwise
     */
    public boolean isNavigating() {
        return isNavigating && !isStopped;
    }

    /**
     * Checks if the Mannequin has reached its navigation target.
     *
     * <p>This checks if the Driver is within the pathfinding acceptance radius of the target.</p>
     *
     * @return true if the target has been reached or navigation has completed
     */
    public boolean hasReachedTarget() {
        if (lastTarget == null || !isNavigating) {
            return false;
        }

        Location driverLocation = driver.getLocation();
        double distance = driverLocation.distance(lastTarget);
        return distance < 2.0; // Acceptance radius
    }

    /**
     * Stops all navigation and cleans up the Driver entity.
     *
     * <p>After calling this method:</p>
     * <ul>
     *   <li>Navigation ceases immediately</li>
     *   <li>The Driver entity is removed from the world</li>
     *   <li>This Navigator instance becomes unusable</li>
     *   <li>Further calls to moveTo() will throw IllegalStateException</li>
     * </ul>
     *
     * <p>This method is safe to call multiple times.</p>
     */
    public void stop() {
        if (isStopped) {
            return;
        }

        isStopped = true;
        isNavigating = false;
    }

    public void remove(){
        if (driver.isValid()) {
            driver.remove();
        }
        if (mannequin.isValid()){
            mannequin.remove();
        }

        manager.unregister(mannequinId);
    }

    /**
     * Resumes navigation toward the last target location.
     *
     * @return true if navigation was resumed, false if there was no previous target
     * @throws IllegalStateException if this navigator has been stopped
     */
    public boolean resume() {
        checkNotStopped();

        if (lastTarget == null) {
            return false;
        }

        return moveTo(lastTarget, lastSpeed);
    }

    /**
     * Gets the Mannequin entity associated with this navigator.
     *
     * @return The Mannequin LivingEntity
     */
    @NotNull
    public LivingEntity getMannequin() {
        return mannequin;
    }

    /**
     * Gets the unique ID of the Mannequin entity.
     *
     * @return The Mannequin's UUID
     */
    @NotNull
    public UUID getMannequinId() {
        return mannequinId;
    }

    /**
     * Gets the Driver mob entity associated with this navigator.
     *
     * @return The Driver Mob entity, or null if the driver has been removed
     */
    @NotNull
    public Zombie getDriver() {
        return driver;
    }

    /**
     * Gets the unique ID of the Driver entity.
     *
     * @return The Driver's UUID
     */
    @NotNull
    public UUID getDriverId() {
        return driverId;
    }
    /**
     * Gets the current navigation target, if any.
     *
     * @return The target location, or null if no navigation is in progress
     */
    @Nullable
    public Location getTarget() {
        return lastTarget;
    }

    /**
     * Gets the speed multiplier used for the current/last navigation.
     *
     * @return The speed multiplier, or 0 if no navigation was attempted
     */
    public double getSpeed() {
        return lastSpeed;
    }

    /**
     * Checks if the Mannequin is currently mounted on its Driver.
     *
     * @return true if the Mannequin is riding the Driver, false otherwise
     */
    public boolean isMounted() {
        return driver.getPassengers().contains(mannequin);
    }

    /**
     * Checks if this navigator has been stopped.
     *
     * @return true if stop() has been called
     */
    public boolean isStopped() {
        return isStopped;
    }

    /**
     * Gets the distance from the Mannequin to the current target.
     *
     * @return Distance in blocks, or -1 if no target is set
     */
    public double distanceToTarget() {
        if (lastTarget == null) {
            return -1;
        }
        return driver.getLocation().distance(lastTarget);
    }

    public void teleport(@NotNull Location location){
        driver.teleport(location);
        sync(true);
    }

    public void sync(boolean force){
        if (!force && !isNavigating()){
            return;
        }

        mannequin.teleport(driver.getLocation());
    }

    public void sync(){
        sync(false);
    }

    /**
     * Checks that this navigator hasn't been stopped.
     *
     * @throws IllegalStateException if stopped
     */
    private void checkNotStopped() {
        if (isStopped) {
            throw new IllegalStateException("This navigator has been stopped and can no longer be used");
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MannequinNavigator that = (MannequinNavigator) o;
        return mannequinId.equals(that.mannequinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mannequinId);
    }

    @Override
    @NotNull
    public String toString() {
        return "MannequinNavigator{" +
                "mannequinId=" + mannequinId +
                ", driverId=" + driverId +
                ", isNavigating=" + isNavigating +
                ", isStopped=" + isStopped +
                ", target=" + (lastTarget != null ?
                    "(" + lastTarget.getX() + "," + lastTarget.getY() + "," + lastTarget.getZ() + ")" :
                    "null") +
                '}';
    }
}
