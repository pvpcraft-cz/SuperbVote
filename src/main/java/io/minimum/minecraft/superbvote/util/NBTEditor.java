package io.minimum.minecraft.superbvote.util;

import net.minecraft.server.v1_8_R3.NBTBase;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagString;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class NBTEditor {

    /**
     * Writes {@link @key} (key) and {@link @value} (value) to the {@link @item} (item's) NBT.
     *
     * @param item  Item that NBT Data's will be changed.
     * @param key   Key of NBT Compound.
     * @param value Value of NBT Compound.
     * @return Edited item. As new.
     */
    public static ItemStack writeNBT(ItemStack item, String key, String value) {
        // Copy of ItemStack(Bukkit)
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        try {
            // Write to compound...
            nbtTagCompound.set(key.toLowerCase().trim(), new NBTTagString(value.toLowerCase().trim()));

            // Save to ItemStack(Minecraft)
            minecraftItemStack.setTag(nbtTagCompound);
            return CraftItemStack.asBukkitCopy(minecraftItemStack);
        } catch (Exception x) {
            return item;
        }
    }

    /**
     * Writes {@link @key} (key) and {@link @value} (value) to the {@link @item} (item's) NBT.
     *
     * @param item Item that NBT Data's will be changed.
     * @param key  Key of NBT Compound.
     * @param <T>  Generic thing
     * @return ItemStack that NBT's will be changed
     */
    public static <T extends NBTBase> ItemStack writeNBT(ItemStack item, String key, T value) {
        // Copy of ItemStack(Bukkit)
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        // write compound
        try {
            nbtTagCompound.set(key, value);
            minecraftItemStack.setTag(nbtTagCompound);

            return CraftItemStack.asBukkitCopy(minecraftItemStack);
        } catch (Exception x) {
            return item;
        }
    }

    /**
     * Gets value from NBT compound that matches {@link @key} from {@link @item} NBT.
     *
     * @param item Item
     * @param key  Key
     * @return String value
     * @throws NullPointerException when did found anything in NBTCompound of item.
     */

    public static String getNBT(ItemStack item, String key) {
        // Copy of ItemStack(Bukkit)
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        try {
            NBTBase baseValue = nbtTagCompound.get(key.toLowerCase().trim());
            if (!Objects.isNull(baseValue)) {
                NBTTagString value = (NBTTagString) baseValue;

                return NBTUtil.strip(value.toString());
            }
        } catch (Exception x) {
            return null;
        }
        return null;
    }

    /**
     * Gets value from NBT compound that matches {@link @key} from {@link @item} NBT.
     *
     * @param item  Item
     * @param key   Key
     * @param clazz Type of NBTBase
     * @param <T>   ...
     * @return NBTBase child that you choose.
     * @throws NullPointerException when did found anything in NBTCompound of item
     */
    public static <T extends NBTBase> T getNBT(ItemStack item, String key, Class<T> clazz) {
        // Copy of ItemStack(Bukkit)
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        try {
            NBTBase baseValue = nbtTagCompound.get(key.toLowerCase().trim());
            T value = (T) baseValue;
            return clazz.cast(value);
        } catch (ClassCastException x) {
            return null;
        }
    }

    /**
     * Removes {@link @key} from {@link @item} NBT.
     *
     * @param item Item that NBT Data's will be changed.
     * @param key  Key of NBT Compound.
     * @return Edited item. As new.
     */
    public static ItemStack removeNBT(ItemStack item, String key) {
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
        if (minecraftItemStack.hasTag()) {
            NBTTagCompound nbtTagCompound = minecraftItemStack.getTag();
            nbtTagCompound.remove(key.toLowerCase().trim());
            minecraftItemStack.setTag(nbtTagCompound);
            return CraftItemStack.asBukkitCopy(minecraftItemStack);
        }
        return item;
    }

    /**
     * Checks if {@link @item} has NBT.
     *
     * @param item Item to check.
     * @return boolean
     */

    public static boolean hasNBT(ItemStack item) {
        try {
            net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);
            return minecraftItemStack.hasTag();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static boolean hasNBTTag(ItemStack item, String tag) {
        if (!hasNBT(item))
            return false;

        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);

        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        return nbtTagCompound.hasKey(tag);
    }

    public static List<String> getNBTKeys(ItemStack item) {
        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);

        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        return new ArrayList<>(nbtTagCompound.c());
    }

    public static HashMap<String, String> getNBT(ItemStack item) {
        HashMap<String, String> nbt = new HashMap<>();

        net.minecraft.server.v1_8_R3.ItemStack minecraftItemStack = CraftItemStack.asNMSCopy(item);

        NBTTagCompound nbtTagCompound = minecraftItemStack.hasTag() ? minecraftItemStack.getTag() : new NBTTagCompound();

        nbtTagCompound.c().forEach(key -> nbt.put(key, nbtTagCompound.get(key.toLowerCase().trim()).toString()));

        return nbt;
    }
}