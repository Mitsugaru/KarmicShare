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

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;


public class BetterChest implements Chest {

    // Methods inherited from BlockState
    // At the moment, these all act upon the reference Chest only.
    public Block getBlock() {
        return ref.getBlock();
    }

    public Chunk getChunk() {
        return ref.getChunk();
    }

    public MaterialData getData() {
        return ref.getData();
    }

    public byte getLightLevel() {
        return ref.getLightLevel();
    }

    public byte getRawData() {
        return ref.getRawData();
    }

    public Material getType() {
        return ref.getType();
    }

    public int getTypeId() {
        return ref.getTypeId();
    }

    public World getWorld() {
        return ref.getWorld();
    }

    public int getX() {
        return ref.getX();
    }

    public int getY() {
        return ref.getY();
    }

    public int getZ() {
        return ref.getZ();
    }

    public void setData(MaterialData data) {
        ref.setData(data);
    }

    public void setType(Material type) {
        ref.setType(type);
    }

    public boolean setTypeId(int type) {
        return ref.setTypeId(type);
    }

    public boolean update() {
        return ref.update();
    }

    public boolean update(boolean force) {
        return ref.update(force);
    }


    // Methods inherited from ContainerBlock
    public Inventory getInventory() {
        Chest other = attached();
        if (other == null) {
            return ref.getInventory();
        }
        else {
            return new DoubleInventory(ref.getInventory(), other.getInventory());
        }
    }

    public boolean isDoubleChest()
    {
    	final Chest other = attached();
    	if(other != null)
    	{
    		return true;
    	}
    	return false;
    }


    // BetterChest internals
    private static final BlockFace[] FACES = {
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    private Chest ref;

    public BetterChest(Chest ref) {
        this.ref = ref;
    }

    public Chest attached() {
        // Find the first adjacent chest. Note: hacking of various sorts/degrees and/or
        // other plugins might allow multiple chests to be adjacent. Deal with that later
        // if it really becomes necessary (and at all possible to detect).

        Block block = ref.getBlock();
        for (BlockFace face : FACES) {
            Block other = block.getRelative(face);
            if (other.getType() == Material.CHEST) {
                return (Chest) other.getState();    // Found it.
            }
        }
        return null;    // No other adjacent chest.
    }

    public Block attachedBlock() {
        // Find the first adjacent chest. Note: hacking of various sorts/degrees and/or
        // other plugins might allow multiple chests to be adjacent. Deal with that later
        // if it really becomes necessary (and at all possible to detect).

        Block block = ref.getBlock();
        for (BlockFace face : FACES) {
            Block other = block.getRelative(face);
            if (other.getType() == Material.CHEST) {
                return other;   // Found it.
            }
        }
        return null;    // No other adjacent chest.
    }

	@Override
	public Location getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRawData(byte arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<MetadataValue> getMetadata(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasMetadata(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeMetadata(String arg0, Plugin arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMetadata(String arg0, MetadataValue arg1) {
		// TODO Auto-generated method stub
		
	}
}