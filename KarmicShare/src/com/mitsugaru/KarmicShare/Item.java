/**
 * Class to represent items that are in the pool
 * Mostly used to help differentiate between
 * items that use damage values
 */
package com.mitsugaru.KarmicShare;

import org.bukkit.Material;
//import org.bukkit.material.Dye;
//import org.bukkit.material.Leaves;
import org.bukkit.material.LongGrass;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Step;
import org.bukkit.material.Tree;
import org.bukkit.material.Wool;

public class Item extends MaterialData {
	// Class variables
	public String name;
	public short durability;

	/**
	 * Constructor
	 *
	 * @param int of item id
	 * @param byte of data value
	 */
	public Item(int i, byte d, short dur) {
		super(i, d);
		durability = dur;
		name = "";
		// Only custom names required for blocks.
		// Not for non-block type entities
		if (this.getItemType().isBlock())
		{
			if (i == 9)
			{
				// Handle water
				name += "water";
			}
			else if (i == 11)
			{
				// Handle lava
				name += "lava";
			}
			else if (d != 0)
			{
				if (i == 6)
				{
					// Handle saplings
					final Tree tree = new Tree(17, d);
					name += tree.getSpecies().toString().toLowerCase()
							.replace('_', ' ').replaceAll("[^a-z ]", "");
					name += " "
							+ this.toString().toLowerCase().replace('_', ' ')
									.replaceAll("[^a-z ]", "");
				}
				else if (i == 17)
				{
					// Grab tree type
					final Tree tree = new Tree(17, d);
					name += tree.toString().toLowerCase().replace('_', ' ')
							.replaceAll("[^a-z ]", "");
				}
				else if (i == 18)
				{
					// Handle leaves
					// final Leaves leaves = new Leaves();
					// INFO Nothing really to do here, seems to handle itself
					// final Tree tree = new Tree(17, d);
					// leaves.setSpecies(tree.getSpecies());
					name += "leaves";
				}
				else if (i == 31)
				{
					// Handle grass
					final LongGrass grass = new LongGrass(31, d);
					name += grass.toString().toLowerCase().replace('_', ' ')
							.replaceAll("[^a-z ]", "");
				}
				else if (i == 35)
				{
					// Grab wool type
					final Wool wool = new Wool(35, d);
					name += wool.toString().toLowerCase().replace('_', ' ')
							.replaceAll("[^a-z ]", "");
				}
				else if (i == 43)
				{
					// Grab double step type
					final Step step = new Step(43, d);
					name += step.toString().toLowerCase().replace('_', ' ')
							.replaceAll("[^a-z ]", "");
				}
				else if (i == 44)
				{
					// Grab step type
					final Step step = new Step(44, d);
					name += step.toString().toLowerCase().replace('_', ' ')
							.replaceAll("[^a-z ]", "");
				}
			}
			else
			{
				final Material mat = Material.getMaterial(i);
				name += mat.toString().toLowerCase().replace('_', ' ')
						.replaceAll("[^a-z ]", "");
			}
		}
		else if (i == 351)
		{
			// Hardcoded names of dye colors
			switch (d)
			{
				case 0:
					name += "ink sac";
					break;
				case 1:
					name += "rose red";
					break;
				case 2:
					name += "cactus green";
					break;
				case 3:
					name += "cocoa beans";
					break;
				case 4:
					name += "lapis lazuli";
					break;
				case 5:
					name += "purple dye";
					break;
				case 6:
					name += "cyan dye";
					break;
				case 7:
					name += "light gray dye";
					break;
				case 8:
					name += "gray dye";
					break;
				case 9:
					name += "pink dye";
					break;
				case 10:
					name += "lime dye";
					break;
				case 11:
					name += "dandelion yellow";
					break;
				case 12:
					name += "light blue dye";
					break;
				case 13:
					name += "magenta dye";
					break;
				case 14:
					name += "orange dye";
					break;
				case 15:
					name += "bone meal";
					break;
				default:
					name += "inc sac";
					break;
			}
		}
		else if (i == 373)
		{
			// Handle potions based on durability
			switch (durability)
			{
				case 0:
					name += "water bottle";
					break;
				case 16:
					name += "awkward potion";
					break;
				case 32:
					name += "thick potion";
					break;
				case 64:
					name += "mundane potion ex";
					break;
				case 8192:
					name += "mundane potion";
					break;
				case 8193:
					name += "regen potion";
					break;
				case 8194:
					name += "swift potion";
					break;
				case 8195:
					name += "fire potion";
					break;
				case 8196:
					name += "poison potion";
					break;
				case 8197:
					name += "healing potion";
					break;
				case 8200:
					name += "weak potion";
					break;
				case 8201:
					name += "strength potion";
					break;
				case 8202:
					name += "slow potion";
					break;
				case 8204:
					name += "harm potion";
					break;
				case 8225:
					name += "regen potion II";
					break;
				case 8226:
					name += "swift potion II";
					break;
				case 8228:
					name += "poison potion II";
					break;
				case 8229:
					name += "healing potion II";
					break;
				case 8233:
					name += "strength potion II";
					break;
				case 8236:
					name += "harm potion II";
					break;
				case 8257:
					name += "regen potion ex";
					break;
				case 8258:
					name += "swift potion ex";
				case 8259:
					name += "fire potion ex";
					break;
				case 8260:
					name += "poison potion ex";
					break;
				case 8264:
					name += "weak potion ex";
					break;
				case 8265:
					name += "strength potion ex";
					break;
				case 8266:
					name += "slow potion ex";
					break;
				case 8289:
					name += "regen potion II ex";
					break;
				case 8290:
					name += "swift potion II ex";
					break;
				case 8292:
					name += "poison potion II ex";
					break;
				case 8297:
					name += "strength potion II ex";
					break;
				case 16384:
					name += "mundane splash potion";
					break;
				case 16385:
					name += "regen splash potion";
					break;
				case 16386:
					name += "swift splash potion";
					break;
				case 16387:
					name += "fire splash potion";
					break;
				case 16388:
					name += "poison splash potion";
					break;
				case 16389:
					name += "heal splash potion";
					break;
				case 16392:
					name += "weak splash potion";
					break;
				case 16393:
					name += "strength splash potion";
					break;
				case 16394:
					name += "slow splash potion";
					break;
				case 16396:
					name += "harm splash potion";
					break;
				case 16417:
					name += "regen splash potion II";
					break;
				case 16418:
					name += "swift splash potion II";
					break;
				case 16420:
					name += "poison splash potion II";
					break;
				case 16421:
					name += "heal splash potion II";
					break;
				case 16425:
					name += "strength splash potion II";
					break;
				case 16428:
					name += "harm splash potion II";
					break;
				case 16449:
					name += "regen splash potion ex";
					break;
				case 16450:
					name += "swift splash potion ex";
					break;
				case 16451:
					name += "fire splash potion ex";
					break;
				case 16452:
					name += "poison splash potion ex";
					break;
				case 16456:
					name += "weak splash potion ex";
					break;
				case 16457:
					name += "strength splash potion ex";
					break;
				case 16458:
					name += "slow splash potion ex";
					break;
				case 16481:
					name += "regen splash potion II ex";
					break;
				case 16482:
					name += "swift splash potion II ex";
					break;
				case 16484:
					name += "poison splash potion II ex";
					break;
				case 16489:
					name += "strength splaash potion II ex";
					break;
				default:
					name += "potion";
					break;
			}
		}
		else if (i == 380)
		{
			// Cauldron
			name += "cauldron";
		}
		// Handle music discs
		else if (i == 2256)
		{
			name += "13 disc";
		}
		else if (i == 2257)
		{
			name += "cat disc";
		}
		else if (i == 2258)
		{
			name += "blocks disc";
		}
		else if (i == 2259)
		{
			name += "chirp disc";
		}
		else if (i == 2260)
		{
			name += "far disc";
		}
		else if (i == 2261)
		{
			name += "mall disc";
		}
		else if (i == 2262)
		{
			name += "mellohi disc";
		}
		else if (i == 2263)
		{
			name += "stal disc";
		}
		else if (i == 2264)
		{
			name += "strad disc";
		}
		else if (i == 2265)
		{
			name += "ward disc";
		}
		else if (i == 2266)
		{
			name += "11 disc";
		}
		else
		{
			// For non special materials, just use Bukkit convention
			final Material mat = Material.getMaterial(i);
			name += mat.toString().toLowerCase().replace('_', ' ')
					.replaceAll("[^a-z ]", "");
		}
	}

	/**
	 * Variant of equals(Object obj)
	 *
	 * @param obj
	 * @return true if they are the same item
	 */
	public boolean areSame(Object obj) {
		// Both blocks
		try
		{
			if (this.getItemType().isBlock()
					&& ((Item) obj).getItemType().isBlock())
			{
				// Check both id and data values
				if (this.getItemTypeId() == ((Item) obj).getItemTypeId())
				{
					if (this.itemId() == 9)
					{
						// Ignore data for leaves
						return true;
					}
					if (this.getData() == ((Item) obj).getData())
					{
						return true;
					}
				}
			}
			else if (!this.getItemType().isBlock()
					&& !((Item) obj).getItemType().isBlock())
			{
				// Both non-block, only check item id
				if (this.getItemTypeId() == ((Item) obj).getItemTypeId())
				{
					//handle if dye or potion
					if(this.itemId() == 351)
					{
						if(this.getData() == ((Item) obj).getData())
						{
							return true;
						}
					}
					else if(this.itemId() == 373)
					{
						if(durability == ((Item) obj).itemDurability())
						{
							return true;
						}
					}
					else
						return true;
				}
			}
		}
		catch (ClassCastException e)
		{
			// Cast failed, so, they're not the same object
			return false;
		}
		return false;
	}

	/**
	 * Method to check if the item is a potion
	 *
	 * @return true if potion, else false;
	 */
	public boolean isPotion() {
		if (this.getItemTypeId() == 373)
			return true;
		return false;
	}

	/**
	 * Method to check if this item is a tool
	 *
	 * @return true if its is a tool item
	 */
	public boolean isTool() {
		return this.isTool(this.getItemTypeId());
	}

	/**
	 * Method that checks a given id to see if its a tool
	 *
	 * @param int of item id
	 * @return true if it matches a known tool, else false
	 */
	public boolean isTool(int id) {
		final int[] tool = { 256, 257, 258, 259, 267, 268, 269, 270, 271, 272,
				273, 274, 275, 276, 277, 278, 279, 283, 284, 285, 286, 290,
				291, 292, 293, 294, 298, 299, 300, 301, 302, 303, 304, 305,
				306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317 };
		if (id >= 256 && id <= 317)
		{
			// within range of "tool" ids
			for (int i = 0; i < tool.length; i++)
			{
				// iterate through array to see if we get a match
				if (id == tool[i])
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Grabs the item id of this Item object
	 *
	 * @return item id
	 */
	public int itemId() {
		return this.getItemType().getId();
	}

	/**
	 * Grabs the data value of this Item object
	 *
	 * @return data value
	 */
	public byte itemData() {
		return this.getData();
	}

	/**
	 * Grabs the durability value of this Item object
	 */
	public short itemDurability() {
		return this.durability;
	}
}
