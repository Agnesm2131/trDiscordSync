package me.agnes.trDiscordSync.util;

import me.agnes.trDiscordSync.trDiscordSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitScheduler;

public class SchedulerUtil {

    private static BukkitScheduler getScheduler() {
        return Bukkit.getScheduler();
    }

    public static void runAsync(Runnable runnable) {
        getScheduler().runTaskAsynchronously(trDiscordSync.getInstance(), runnable);
    }

    public static void runSync(Runnable runnable) {
        getScheduler().runTask(trDiscordSync.getInstance(), runnable);
    }

    public static void runEntitySync(Entity entity, Runnable runnable) {
        getScheduler().runTask(trDiscordSync.getInstance(), runnable);
    }

    public static void runLaterAsync(Runnable runnable, long ticks) {
        getScheduler().runTaskLaterAsynchronously(trDiscordSync.getInstance(), runnable, ticks);
    }

    public static void runLaterSync(Runnable runnable, long ticks) {
        getScheduler().runTaskLater(trDiscordSync.getInstance(), runnable, ticks);
    }

    public static void runTimerAsync(Runnable runnable, long delayTicks, long periodTicks) {
        getScheduler().runTaskTimerAsynchronously(trDiscordSync.getInstance(), runnable, delayTicks, periodTicks);
    }

    public static void runTimerSync(Runnable runnable, long delayTicks, long periodTicks) {
        getScheduler().runTaskTimer(trDiscordSync.getInstance(), runnable, delayTicks, periodTicks);
    }
}
