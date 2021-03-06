package io.minimum.minecraft.superbvote.commands.gui;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import space.devport.utils.configuration.Configuration;
import space.devport.utils.item.ItemBuilder;
import space.devport.utils.item.ItemNBTEditor;
import space.devport.utils.text.Placeholders;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ClaimGUI implements Listener {

    private final SuperbVote plugin;

    private final Configuration config;

    private Player player;
    private Inventory inv;

    public ClaimGUI() {
        this.plugin = SuperbVote.getPlugin();
        this.config = new Configuration(SuperbVote.getPlugin(), "config");
    }

    public void open() {
        player.openInventory(inv);
    }

    // Build the gui
    public void build(Player player) {
        this.player = player;

        inv = Bukkit.createInventory(null, config.getFileConfiguration().getInt("claim.gui.size"), config.getColoredString("claim.gui.title", "&r"));

        // Start building the items by matrix.
        String[] matrix = config.getArrayList("claim.gui.matrix");

        Placeholders placeholders = new Placeholders()
                .add("%votes%", SuperbVote.getPlugin().getQueuedVotes().getVotes(player.getUniqueId()).size());

        // Prepare builders
        ItemBuilder closeBuild = config.getItemBuilder("claim.gui.items.close", new ItemBuilder(Material.AIR))
                .addNBT("superbvote_gui", "close")
                .parseWith(placeholders);
        ItemBuilder fillerBuild = config.getItemBuilder("claim.gui.items.filler", new ItemBuilder(Material.AIR))
                .addNBT("superbvote_gui", "filler")
                .parseWith(placeholders);
        ItemBuilder gatherBuild = config.getItemBuilder("claim.gui.items.gather", new ItemBuilder(Material.AIR))
                .addNBT("superbvote_gui", "gather")
                .parseWith(placeholders);

        int slot = 0;

        for (String line : matrix) {
            for (char c : line.toCharArray()) {
                switch (c) {
                    case 'g':
                        inv.setItem(slot, gatherBuild.build());
                        break;
                    case 'c':
                        inv.setItem(slot, closeBuild.build());
                        break;
                    case ' ':
                        inv.setItem(slot, fillerBuild.build());
                        break;
                }

                slot++;
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        // Filter nulls
        if (e.getCurrentItem() == null || e.getCursor() == null || e.getClickedInventory() == null)
            return;

        // Check inv
        if (e.getClickedInventory().equals(inv)) {
            // Cancel
            e.setCancelled(true);

            ItemStack item = e.getCurrentItem();

            if (!ItemNBTEditor.hasNBT(item))
                return;

            if (!ItemNBTEditor.hasNBTKey(item, "superbvote"))
                return;

            String value = ItemNBTEditor.getString(item, "superbvote");

            switch (value.trim().toLowerCase()) {
                case "gather":
                    claim(player);
                    player.closeInventory();
                    break;
                case "close":
                    player.closeInventory();
                    break;
                case "filler":
                default:
            }
        }
    }

    private void claim(Player player) {
        UUID uuid = player.getUniqueId();

        PlayerVotes pv = SuperbVote.getPlugin().getVoteStorage().getVotes(uuid);
        List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(uuid);

        if (votes.size() == 0) {
            player.sendMessage(ChatColor.RED + "You have no votes to claim.");
            return;
        }

        int spaceRequired = SuperbVote.getPlugin().getConfig().getInt("claim.inv-space-per-vote");

        Iterator<Vote> iter = votes.iterator();

        int claimed = 0;

        while (iter.hasNext()) {
            if (spaceRequired > countInventorySpace(player.getInventory()))
                break;

            Vote v = iter.next();

            SuperbVote.getPlugin().getVoteService().processVote(pv, v, false, false, true, false);
            pv = new PlayerVotes(pv.getUuid(), player.getName(), pv.getVotes() + 1, PlayerVotes.Type.CURRENT);

            votes.remove(v);
            claimed++;
        }

        // Add unclaimed votes back
        // Unreliable, but has to do for now, will later replace with a better and separate queued-vote get method. Maybe..
        votes.forEach(v -> SuperbVote.getPlugin().getQueuedVotes().addVote(v));

        Configuration cfg = new Configuration(SuperbVote.getPlugin(), "config");

        List<String> msg = cfg.getColoredList("claim.claimed");

        for (String line : msg) {
            line = line.replace("%votes%", String.valueOf(claimed));

            if (line.contains("%remaining_votes%") && votes.size() != 0)
                player.sendMessage(line.replace("%remaining_votes%", String.valueOf(votes.size())));
            else if (!line.contains("%remaining_votes%"))
                player.sendMessage(line);
        }
    }

    private int countInventorySpace(Inventory inv) {
        int n = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType().equals(Material.AIR))
                n++;
        }

        return n;
    }
}