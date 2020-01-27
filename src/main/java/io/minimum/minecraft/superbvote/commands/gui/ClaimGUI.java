package io.minimum.minecraft.superbvote.commands.gui;

import io.minimum.minecraft.superbvote.util.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ClaimGUI implements Listener {

    private Configuration config;

    public ClaimGUI() {

    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        // Filter nulls
        if (e.getCurrentItem() == null || e.getCursor() == null || e.getInventory() == null || e.getWhoClicked() == null)
            return;


    }
}
