/*
    Copyright (C) 2011 by Matthew D Moss

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/

/**
 * @author mattmoss
 */
package com.splatbang.betterchest;

import java.util.Arrays;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


public class DoubleInventory implements Inventory {

    // Methods inherited from Inventory
    public int getSize() {
        return major.getSize() + minor.getSize();
    }

    public String getName() {
        return major.getName() + ":" + minor.getName();
    }

    public ItemStack getItem(int index) {
        int majorSize = major.getSize();
        if (index < majorSize)
            return major.getItem(index);
        else
            return minor.getItem(index - majorSize);
    }

    public void setItem(int index, ItemStack item) {
        int majorSize = major.getSize();
        if (index < majorSize)
            major.setItem(index, item);
        else
            minor.setItem(index - majorSize, item);
    }

    public HashMap<Integer, ItemStack> addItem(ItemStack... items) {
        // TODO: Reasonable implementation?
        HashMap<Integer, ItemStack> leftover = major.addItem(items);
        if (leftover != null && !leftover.isEmpty()) {
            ItemStack[] rest = { }; // not null
            rest = leftover.values().toArray(rest);
            leftover = minor.addItem(rest);
        }
        return leftover;
    }

    public HashMap<Integer, ItemStack> removeItem(ItemStack... items) {
        // TODO: Reasonable implementation?
        HashMap<Integer, ItemStack> leftover = major.addItem(items);
        if (leftover != null && !leftover.isEmpty()) {
            ItemStack[] rest = { }; // not null
            rest = leftover.values().toArray(rest);
            leftover = minor.removeItem(rest);
        }
        return leftover;
    }

    public ItemStack[] getContents() {
        return concat(major.getContents(), minor.getContents());
    }

    public void setContents(ItemStack[] items) {
        // TODO: Reasonable implementation?
        int majorSize = major.getSize();
        if (items.length <= majorSize) {
            major.setContents(items);
        }
        else {
            major.setContents(Arrays.copyOfRange(items, 0, majorSize));
            minor.setContents(Arrays.copyOfRange(items, majorSize, items.length - majorSize));
        }
    }

    public boolean contains(int materialId) {
        return major.contains(materialId) || minor.contains(materialId);
    }

    public boolean contains(Material material) {
        return major.contains(material) || minor.contains(material);
    }

    public boolean contains(ItemStack item) {
        return major.contains(item) || minor.contains(item);
    }

    public boolean contains(int materialId, int amount) {
        return major.contains(materialId, amount) || minor.contains(materialId, amount);
    }

    public boolean contains(Material material, int amount) {
        return major.contains(material, amount) || minor.contains(material, amount);
    }

    public boolean contains(ItemStack item, int amount) {
        return major.contains(item, amount) || minor.contains(item, amount);
    }

    public HashMap<Integer, ? extends ItemStack> all(int materialId) {
        return combineWithOffset(major.all(materialId), minor.all(materialId), major.getSize());
    }

    public HashMap<Integer, ? extends ItemStack> all(Material material) {
        return combineWithOffset(major.all(material), minor.all(material), major.getSize());
    }

    public HashMap<Integer, ? extends ItemStack> all(ItemStack item) {
        return combineWithOffset(major.all(item), minor.all(item), major.getSize());
    }

    public int first(int materialId) {
        int majorSize = major.getSize();
        int index = major.first(materialId);
        if (index < 0) {
            index = minor.first(materialId);
            if (index >= 0)
                index += majorSize;
        }
        return index;
    }

    public int first(Material material) {
        int majorSize = major.getSize();
        int index = major.first(material);
        if (index < 0) {
            index = minor.first(material);
            if (index >= 0)
                index += majorSize;
        }
        return index;
    }

    public int first(ItemStack item) {
        int majorSize = major.getSize();
        int index = major.first(item);
        if (index < 0) {
            index = minor.first(item);
            if (index >= 0)
                index += majorSize;
        }
        return index;
    }

    public int firstEmpty() {
        int majorSize = major.getSize();
        int index = major.firstEmpty();
        if (index < 0) {
            index = minor.firstEmpty();
            if (index >= 0)
                index += majorSize;
        }
        return index;
    }

    public void remove(int materialId) {
        major.remove(materialId);
        minor.remove(materialId);
    }

    public void remove(Material material) {
        major.remove(material);
        minor.remove(material);
    }

    public void remove(ItemStack item) {
        major.remove(item);
        minor.remove(item);
    }

    public void clear(int index) {
        int majorSize = major.getSize();
        if (index < majorSize)
            major.clear(index);
        else
            minor.clear(index - majorSize);
    }

    public void clear() {
        major.clear();
        minor.clear();
    }


    // DoubleInventory internals
    private Inventory major;
    private Inventory minor;

    public DoubleInventory(Inventory major, Inventory minor) {
        this.major = major;
        this.minor = minor;
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static <T> HashMap<Integer, ? extends T> combineWithOffset(HashMap<Integer, ? extends T> first,
                                                                       HashMap<Integer, ? extends T> second,
                                                                       int offset) {
        // TODO: Reasonable implementation?
        HashMap<Integer, T> result = new HashMap<Integer, T>(first);

        // Put in items from the second map, adjusting key values.
        for (Integer key : second.keySet()) {
            result.put(new Integer(key.intValue() + offset), (T) second.get(key));
        }

        return result;
    }
}