package me.agnes.trDiscordSync.listener;

import me.agnes.trDiscordSync.trDiscordSync;
import me.agnes.trDiscordSync.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    private final trDiscordSync plugin;

    public MenuListener(trDiscordSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();

        String menuTitle = MessageUtil
                .stripColors(MessageUtil.color(plugin.getGuiConfig().profileMenu.title.split(":")[0]));
        String currentTitle = MessageUtil.stripColors(title);

        if (currentTitle.startsWith(menuTitle)) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
                return;

            if (event.getCurrentItem().getType() == Material.BARRIER) {
                event.getWhoClicked().closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        String menuTitle = MessageUtil
                .stripColors(MessageUtil.color(plugin.getGuiConfig().profileMenu.title.split(":")[0]));
        String currentTitle = MessageUtil.stripColors(title);

        if (currentTitle.startsWith(menuTitle)) {
            event.setCancelled(true);
        }
    }
}
