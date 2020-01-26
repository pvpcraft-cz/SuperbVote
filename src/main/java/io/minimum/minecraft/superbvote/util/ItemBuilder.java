package io.minimum.minecraft.superbvote.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemBuilder {

    private Material type;
    private short damage = 0;

    private String displayName;

    private int amount = 1;

    private List<String> lore = new ArrayList<>();

    private boolean glow = false;

    private HashMap<Enchantment, Integer> enchants = new HashMap<>();

    private List<ItemFlag> flags = new ArrayList<>();

    private HashMap<String, String> NBT = new HashMap<>();

    public ItemBuilder(Material type) {
        this.type = type;
    }

    public ItemBuilder(ItemStack item) {
        this.type = item.getType();

        this.damage = (byte) item.getDurability();

        this.amount = item.getAmount();

        if (item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta.hasDisplayName())
                this.displayName = itemMeta.getDisplayName();

            if (itemMeta.hasLore())
                this.lore = itemMeta.getLore();

            if (itemMeta.hasEnchants())
                this.enchants = new HashMap<>(itemMeta.getEnchants());

            this.flags = new ArrayList<>(itemMeta.getItemFlags());
        }
    }

    public static ItemBuilder loadBuilder(FileConfiguration yaml, String path) {
        try {
            ConfigurationSection section = yaml.getConfigurationSection(path);

            String type = section.getString("Type");
            Material mat = Material.valueOf(type);

            short data = (short) section.getInt("Damage");

            ItemBuilder b = new ItemBuilder(mat).damage(data);

            if (section.contains("Name"))
                b.displayName(section.getString("Name"));

            if (section.contains("Amount"))
                b.amount(section.getInt("Amount"));

            if (section.contains("Glow"))
                b.setGlow(section.getBoolean("Glow"));

            if (section.contains("Lore"))
                b.lore(section.getStringList("Lore"));

            return b;
        } catch (NullPointerException | IllegalArgumentException e) {
            return new ItemBuilder(Material.STONE).displayName("&cCould not load item").addLine("&7Reason: &c" + e.getMessage());
        }
    }

    public ItemBuilder parse(String str, String str1) {

        if (displayName != null)
            displayName = displayName.replace(str, str1);

        if (lore != null)
            if (!lore.isEmpty()) {
                List<String> newLore = new ArrayList<>();

                for (String line : lore)
                    newLore.add(line.replace(str, str1));

                lore = newLore;
            }

        return this;
    }

    public ItemBuilder addNBT(String key, String value) {
        NBT.put(key, value);
        return this;
    }

    public ItemBuilder removeNBT(String key) {
        NBT.remove(key);
        return this;
    }

    public ItemBuilder clearEnchants() {
        this.enchants = new HashMap<>();
        return this;
    }

    public ItemBuilder clearFlags() {
        this.flags = new ArrayList<>();
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemBuilder damage(short data) {
        this.damage = data;
        return this;
    }

    public ItemBuilder lore(String[] lore) {
        for (String line : lore)
            this.lore.add(line);
        return this;
    }

    public ItemBuilder addLine(String str) {
        lore.add(str);
        return this;
    }

    public ItemBuilder addFlag(ItemFlag flag) {
        flags.add(flag);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        enchants.put(enchantment, level);
        return this;
    }

    public ItemBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemBuilder setGlow(boolean glow) {
        this.glow = glow;
        return this;
    }

    public ItemBuilder type(Material mat) {
        this.type = mat;
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(type, amount, damage);

        ItemMeta meta = item.getItemMeta();

        if (!lore.isEmpty()) {
            // Color it
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                lore.set(i, color(line));
            }

            meta.setLore(lore);
        }

        if (displayName != null)
            meta.setDisplayName(color(displayName));

        if (!enchants.isEmpty()) {
            for (Enchantment enchantment : enchants.keySet()) {
                meta.addEnchant(enchantment, enchants.get(enchantment), true);
            }
        }

        if (!flags.isEmpty()) {
            for (ItemFlag flag : flags)
                meta.addItemFlags(flag);
        }

        if (glow) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);

        // NBT
        if (!NBT.isEmpty()) {
            for (String key : NBT.keySet()) {
                item = NBTEditor.writeNBT(item, key, NBT.get(key));
            }
        }

        return item;
    }

    private String color(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public Material type() {
        return type;
    }

    public short damage() {
        return damage;
    }

    public String displayName() {
        return displayName;
    }

    public int amount() {
        return amount;
    }

    public List<String> lore() {
        return lore;
    }

    public boolean glow() {
        return glow;
    }

    public HashMap<Enchantment, Integer> enchants() {
        return enchants;
    }

    public List<ItemFlag> flags() {
        return flags;
    }
}
