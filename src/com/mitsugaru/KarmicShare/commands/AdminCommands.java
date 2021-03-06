package com.mitsugaru.KarmicShare.commands;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.database.Table;
import com.mitsugaru.KarmicShare.database.SQLibrary.Query;
import com.mitsugaru.KarmicShare.inventory.Item;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.permissions.PermissionHandler;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;
import com.mitsugaru.KarmicShare.prompts.CleanupQuestion;
import com.mitsugaru.KarmicShare.prompts.DrainQuestion;
import com.mitsugaru.KarmicShare.prompts.PlayerKarmaResetQuestion;
import com.mitsugaru.KarmicShare.prompts.RemoveGroupQuestion;
import com.mitsugaru.KarmicShare.tasks.RemoveGroupTask;

class AdminCommands{
   private static KarmicShare plugin;
   private static ConversationFactory factory;

   static void init(KarmicShare ks){
      plugin = ks;
      factory = new ConversationFactory(ks);
   }

   static void parseCommand(CommandSender sender, String[] args){
      if(args.length > 1){
         // They have a parameter, thus
         // parse in adminCommand method
         if(!adminCommand(sender, args)){
            // Bad command
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Syntax error. Use /ks admin for list of commands");
         }
      }else{
         // Show admin commands help menu
         sender.sendMessage(ChatColor.BLUE + "===" + ChatColor.RED
               + "KarmicShare Admin" + ChatColor.BLUE + "===");
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_ADD)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin add <item>[:data] [amount]" + ChatColor.YELLOW
                  + " : Add item(s) to pool");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_RESET)){
            sender.sendMessage(ChatColor.GREEN + "/ks admin reset <player>"
                  + ChatColor.YELLOW + " : Resets player's karma");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_SET)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin set <player> <karma>" + ChatColor.YELLOW
                  + " : Sets player's karma to value");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_DRAIN)){
            sender.sendMessage(ChatColor.GREEN + "/ks admin drain [group]"
                  + ChatColor.YELLOW + " : Empty item pool");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_CREATE)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin group create <group>" + ChatColor.YELLOW
                  + " : Create group in database");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_DELETE)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin group delete <group>" + ChatColor.YELLOW
                  + " : Remove group from database");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_ADD)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin group add  <group> <player> [player2] ..."
                  + ChatColor.YELLOW + " : Force add player to group");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_REMOVE)){
            sender.sendMessage(ChatColor.GREEN
                  + "/ks admin group remove  <group> <player> [player2] ..."
                  + ChatColor.YELLOW + " : Force remove player to group");
         }
         if(PermissionHandler.has(sender, PermissionNode.ADMIN_CLEANUP)){
            sender.sendMessage(ChatColor.GREEN + "/ks admin cleanup"
                  + ChatColor.YELLOW + " : Run cleanup task");
         }
         if(PermissionHandler.has(sender, "KarmicShare.admin.reload")){
            sender.sendMessage(ChatColor.GREEN + "/ks admin reload"
                  + ChatColor.YELLOW + " : Reload configuration");
         }
      }
   }

   private static boolean adminCommand(CommandSender sender, String[] args){
      final String com = args[1];
      // Add generated items to pool
      if(com.equals("add")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_ADD)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: " + PermissionNode.ADMIN_ADD.getNode());
            return true;
         }
         if(args.length <= 2){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: " + PermissionNode.ADMIN_ADD.getNode());
            return true;
         }
         int itemid = 0, data = 0, amount = 1;
         short dur = 0;
         try{
            itemid = Integer.parseInt(args[2]);
         }catch(NumberFormatException e){
            if(args[2].contains(":")){
               // Attempt to parse as itemid:data
               String[] cut = args[2].split(":");
               try{
                  itemid = Integer.parseInt(cut[0]);
                  data = Integer.parseInt(cut[1]);
                  dur = Short.parseShort(cut[1]);
               }catch(NumberFormatException r){
                  // Not a number given
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Invalid item id / data value");
                  return false;
               }
            }else{
               // They gave a non-integer
               // Try and parse the string as material
               final Material mat = Material.matchMaterial(args[2]);
               if(mat == null){
                  // Not a known material
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Item name/id is incorrect.");
                  return false;
               }else{
                  itemid = mat.getId();
               }
            }
         }

         // They specified an amount
         if(args.length > 3){
            // Ignore rest of arguments and parse the
            // immediate after
            try{
               amount = Integer.parseInt(args[3]);
            }catch(NumberFormatException n){
               // Not a number given
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Invalid item amount");
               return false;
            }
         }
         // Grab group
         String group = Karma.selectedGroup.get(sender.getName());
         if(group == null){
            Karma.selectedGroup.put(sender.getName(), "global");
            group = "global";
         }
         final int groupId = Karma.getGroupId(group);
         if(groupId == -1){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Unknown group '" + ChatColor.GRAY + group + ChatColor.RED
                  + "'");
            return true;
         }
         // Create item object
         final Item item = new Item(itemid, Byte.valueOf("" + data), dur);
         if(itemid != 0){
            // If there is no item, stop
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Cannot add air to pool.");
            return true;
         }
         if(item.isPotion()){
            data = 0;
            // Create SQL query to see if item is already in
            // database
            String query = "SELECT * FROM " + Table.ITEMS.getName()
                  + " WHERE itemid='" + itemid + "' AND durability='" + dur
                  + "' AND groups='" + groupId + "';";
            Query rs = plugin.getDatabaseHandler().select(query);
            // Send Item to database
            try{
               if(rs.getResult().next()){
                  // here you know that there is at least
                  // one record
                  do{
                     int total = amount + rs.getResult().getInt("amount");
                     query = "UPDATE " + Table.ITEMS.getName()
                           + " SET amount='" + total + "' WHERE itemid='"
                           + itemid + "' AND durability='" + dur
                           + "' AND groups='" + groupId + "';";
                  }while(rs.getResult().next());
               }else{
                  // Item not in database, therefore add
                  // it
                  query = "INSERT INTO " + Table.ITEMS.getName()
                        + " (itemid,amount,data, durability,groups) VALUES ("
                        + itemid + "," + amount + "," + data + "," + dur + ",'"
                        + groupId + "');";
               }
               rs.getResult().close();
               plugin.getDatabaseHandler().standardQuery(query);
               sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Added "
                     + ChatColor.GOLD + amount + ChatColor.GREEN + " of "
                     + ChatColor.AQUA + item.name + ChatColor.GREEN + " to "
                     + ChatColor.GRAY + group + ChatColor.GREEN + ".");
            }catch(SQLException q){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + "Could not add item to pool!");
               q.printStackTrace();
            }
         }else{
            // Create SQL query to see if item is already in
            // database
            String query = "SELECT * FROM " + Table.ITEMS.getName()
                  + " WHERE itemid='" + itemid + "' AND data='" + data
                  + "' AND groups='" + groupId + "';";
            Query rs = plugin.getDatabaseHandler().select(query);
            // Send Item to database
            try{
               if(rs.getResult().next()){
                  // here you know that there is at least
                  // one record
                  do{
                     int total = amount + rs.getResult().getInt("amount");
                     query = "UPDATE " + Table.ITEMS.getName()
                           + " SET amount='" + total + "' WHERE itemid='"
                           + itemid + "' AND data='" + data + "' AND groups='"
                           + groupId + "';";
                  }while(rs.getResult().next());
               }else{
                  // Item not in database, therefore add
                  // it
                  query = "INSERT INTO " + Table.ITEMS.getName()
                        + " (itemid,amount,data, durability,groups) VALUES ("
                        + itemid + "," + amount + "," + data + "," + dur + ",'"
                        + groupId + "');";
               }
               rs.closeQuery();
               plugin.getDatabaseHandler().standardQuery(query);
               sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Added "
                     + ChatColor.GOLD + amount + ChatColor.GREEN + " of "
                     + ChatColor.AQUA + item.name + ChatColor.GREEN + " to "
                     + ChatColor.GRAY + group + ChatColor.GREEN + ".");
            }catch(SQLException q){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Could not add item to pool!");
               q.printStackTrace();
            }
         }
         return true;
      }else if(com.equals("drain")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_DRAIN)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: " + PermissionNode.ADMIN_DRAIN.getNode());
            return true;
         }
         String group = "global";
         // Check if group name was given
         if(args.length > 2){
            group = args[2].toLowerCase();
            if(!Karma.validGroup(sender, args[2])){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Group "
                     + ChatColor.GRAY + group + ChatColor.RED
                     + " does not exist");
               return true;
            }
         }
         if(sender instanceof Player){
            final Map<Object, Object> map = new HashMap<Object, Object>();
            map.put("question", ChatColor.YELLOW + KarmicShare.TAG
                  + ChatColor.DARK_AQUA + " Delete ALL items in "
                  + ChatColor.GOLD + group + ChatColor.DARK_AQUA
                  + " pool? No recovery...");
            map.put("plugin", plugin);
            map.put("group", group);
            factory.withFirstPrompt(new DrainQuestion())
                  .withInitialSessionData(map).withLocalEcho(false)
                  .buildConversation((Player) sender).begin();
         }else{
            // Sent from console
            // Wipe table
            final int id = Karma.getGroupId(group);
            if(id != -1){
               final String query = "DELETE FROM " + Table.ITEMS.getName()
                     + " WHERE groups='" + id + "';";
               plugin.getDatabaseHandler().standardQuery(query);
               plugin.getLogger().info(
                     "Items for group '" + group + "' cleared");
            }else{
               sender.sendMessage(group + " id not found.");
            }
         }
         return true;
      }else if(com.equals("cleanup")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_CLEANUP)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: "
                  + PermissionNode.ADMIN_CLEANUP.getNode());
            return true;
         }
         if(sender instanceof Player){
            final Map<Object, Object> map = new HashMap<Object, Object>();
            map.put("question", ChatColor.YELLOW + KarmicShare.TAG
                  + ChatColor.DARK_AQUA + " Run cleanup task?");
            map.put("plugin", plugin);
            factory.withFirstPrompt(new CleanupQuestion())
                  .withInitialSessionData(map).withLocalEcho(false)
                  .buildConversation((Player) sender).begin();
         }else{
            // Sent from console
            plugin.getDatabaseHandler().standardQuery(
                  "DELETE FROM " + Table.ITEMS.getName()
                        + " WHERE amount<='0';");
            plugin.getLogger().info("Cleanup query executed");
         }
         return true;
      }else if(com.equals("reload")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_RELOAD)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: "
                  + PermissionNode.ADMIN_RELOAD.getNode());
            return true;
         }
         RootConfig.reload();
         sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
               + " Config reloaded");
         Karma.multiPage.clear();
         return true;
      }else if(com.equals("reset")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_RESET)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: " + PermissionNode.ADMIN_RESET.getNode());
            return true;
         }
         if(RootConfig.getBoolean(ConfigNode.KARMA_DISABLED)){
            // Karma system disabled
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Karma disabled.");
            return true;
         }else if(RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY)){
            sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
                  + " Uses economy system");
            return true;
         }
         // Check if name was given
         if(args.length <= 2){
            // did not give a player name, therefore error
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " No player name given.");
            return false;
         }
         // attempt to parse name
         String name = plugin.expandName(args[2]);
         if(name == null){
            name = args[2];
         }
         if(Karma.hasPlayerFromPartialName(sender, name)){
            if(sender instanceof Player){
               final Map<Object, Object> map = new HashMap<Object, Object>();
               map.put("question", ChatColor.YELLOW + KarmicShare.TAG
                     + ChatColor.DARK_AQUA + " Reset " + ChatColor.GOLD + name
                     + ChatColor.DARK_AQUA + "'s karma?");
               map.put("name", name);
               factory.withFirstPrompt(new PlayerKarmaResetQuestion())
                     .withInitialSessionData(map).withLocalEcho(false)
                     .buildConversation((Player) sender).begin();
            }else{
               Karma.setPlayerKarma(name,
                     RootConfig.getInt(ConfigNode.KARMA_PLAYER_DEFAULT));
               sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG + " "
                     + name + "'s karma reset");
            }
         }
         return true;
      }else if(com.equals("set")){
         if(!PermissionHandler.has(sender, PermissionNode.ADMIN_SET)){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Lack permission: " + PermissionNode.ADMIN_SET.getNode());
            return true;
         }
         if(RootConfig.getBoolean(ConfigNode.KARMA_DISABLED)){
            // Karma disabled
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Karma disabled.");
            return true;
         }else if(RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY)){
            sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
                  + " Uses economy system");
            return true;
         }
         // Check if name was given
         if(args.length <= 2){
            // did not give a player name, therefore error
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " No player name given.");
            return false;
         }
         // attempt to parse name
         String name = plugin.expandName(args[2]);
         if(name == null){
            name = args[2];
         }
         // Check if amount was given
         if(args.length <= 3){
            // did not give a karma value, therefore error
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " No karma amount given.");
            return false;
         }
         // Attempt to parse amount
         int amount = 0;
         try{
            amount = Integer.parseInt(args[3]);
         }catch(NumberFormatException e){
            // Invalid integer given for amount
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG + args[2]
                  + " is not a valid integer");
            return false;
         }
         if(Karma.hasPlayerFromPartialName(sender, name)){
            Karma.setPlayerKarma(name, amount);
            sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG + " " + name
                  + "'s karma set");
         }
         return true;
      }
      // Admin command for groups
      else if(com.equals("group")){
         // Check for second com
         if(args.length <= 2){
            // did not give a player name, therefore error
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Missing arguments. Use /ks admin for help");
            return false;
         }
         final String groupCom = args[2];
         if(groupCom.equals("delete")){
            if(!PermissionHandler
                  .has(sender, PermissionNode.ADMIN_GROUP_DELETE)){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.ADMIN_GROUP_DELETE.getNode());
               return true;
            }
            if(args.length <= 3){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Missing group name.");
               return false;
            }
            final String group = args[3].toLowerCase();
            if(group.equals("global")){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Cannot remove the global group.");
               return true;
            }else if(group.startsWith("self_")){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Cannot remove the self group.");
               return true;
            }
            if(!Karma.validGroup(sender, group)){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Group "
                     + ChatColor.GRAY + group + ChatColor.RED
                     + " does not exist");
               return true;
            }
            if(sender instanceof Player){
               final Map<Object, Object> map = new HashMap<Object, Object>();
               map.put("question", ChatColor.YELLOW + KarmicShare.TAG
                     + ChatColor.DARK_AQUA + " Remove group " + ChatColor.GOLD
                     + group + ChatColor.DARK_AQUA + "? ");
               map.put("plugin", plugin);
               map.put("group", group);
               factory.withFirstPrompt(new RemoveGroupQuestion())
                     .withInitialSessionData(map).withLocalEcho(false)
                     .buildConversation((Player) sender).begin();
            }else{
               // Sent via console
               int i = plugin
                     .getServer()
                     .getScheduler()
                     .scheduleAsyncDelayedTask(plugin,
                           new RemoveGroupTask(plugin, sender, group));
               if(i == -1){
                  sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
                        + " Could not schedule task.");
               }
               plugin.getDatabaseHandler().standardQuery(
                     "DELETE FROM " + Table.ITEMS.getName() + " WHERE groups='"
                           + group + "';");
               sender.sendMessage(KarmicShare.TAG
                     + " Removed all items of group: " + group);
            }
            return true;
         }else if(groupCom.equals("create")){
            if(!PermissionHandler
                  .has(sender, PermissionNode.ADMIN_GROUP_CREATE)){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.ADMIN_GROUP_CREATE.getNode());
               return true;
            }
            if(args.length <= 3){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Missing group name.");
               return false;
            }
            final String group = args[3].toLowerCase();
            if(!Karma.validGroup(sender, group)){
               // Create group
               plugin.getDatabaseHandler().standardQuery(
                     "INSERT INTO " + Table.GROUPS.getName()
                           + " (groupname) VALUES ('" + group + "');");
               sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG + " Group "
                     + ChatColor.GRAY + group + ChatColor.GREEN + " created");
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Group "
                     + ChatColor.GRAY + group + ChatColor.RED
                     + " already exists");
            }
            return true;
         }else if(groupCom.equals("add")){
            if(!PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_ADD)){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.ADMIN_GROUP_CREATE.getNode());
               return true;
            }
            try{
               String group = "";
               if(args.length > 3){
                  // Grab group name if given
                  // force group names to lower case
                  group = args[3].toLowerCase();
                  if(!group.matches(Karma.GROUP_NAME_REGEX)){
                     sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                           + " Group name must be alphanumeric");
                     return true;
                  }
               }else{
                  // Group name was not given
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Admin command must specify group");
                  return false;
               }
               if(args.length <= 4){
                  // Player name was not given
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Admin command must specify player(s)");
                  return false;
               }
               for(int i = 4; i < args.length; i++){
                  String name = plugin.expandName(args[i]);
                  if(name == null){
                     name = args[i];
                  }
                  if(Karma.playerHasGroup(sender, name, group)){
                     sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
                           + " " + ChatColor.AQUA + name + ChatColor.YELLOW
                           + " is already in " + ChatColor.GRAY + group);
                     return true;
                  }else{
                     if(Karma.validGroup(sender, group)){
                        // add player to group
                        Karma.addPlayerToGroup(sender, name, group);
                        sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
                              + " Added " + ChatColor.GOLD + name
                              + ChatColor.GREEN + " to " + ChatColor.GRAY
                              + group);
                        final Player p = plugin.getServer().getPlayer("name");
                        if(p != null){
                           p.sendMessage(ChatColor.GREEN + KarmicShare.TAG
                                 + " You have been added to " + ChatColor.GRAY
                                 + group);
                        }
                     }else{
                        sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                              + " Group " + ChatColor.GRAY + group
                              + ChatColor.RED + " does not exist");
                     }
                  }
               }
            }catch(IndexOutOfBoundsException e){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Player name not given");
               return false;
            }
            return true;
         }else if(com.equals("remove")){
            if(!PermissionHandler
                  .has(sender, PermissionNode.ADMIN_GROUP_REMOVE)){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.ADMIN_GROUP_REMOVE.getNode());
               return true;
            }
            try{
               // Grab group name if given
               String group = "";
               if(args.length <= 3){
                  // Group name was not given
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Admin must specify group");
                  return false;
               }
               // force group names to lower case
               group = args[3].toLowerCase();
               if(!group.matches(Karma.GROUP_NAME_REGEX)){
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Group name must be alphanumeric");
                  return true;
               }
               if(args.length <= 4){
                  // Player name was not given
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Admin must specify player(s)");
                  return false;
               }
               for(int i = 4; i < args.length; i++){
                  String name = plugin.expandName(args[i]);
                  if(name == null){
                     name = args[i];
                  }
                  if(!Karma.playerHasGroup(sender, name, group)){
                     sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
                           + ChatColor.AQUA + name + ChatColor.YELLOW
                           + " not in " + ChatColor.GRAY + group);
                     return true;
                  }else{
                     if(Karma.validGroup(sender, group)){
                        // remove other player to group
                        Karma.removePlayerFromGroup(sender, name, group);
                        sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
                              + " Removed " + ChatColor.GOLD + name
                              + ChatColor.GREEN + " from " + ChatColor.GRAY
                              + group);
                        final Player p = plugin.getServer().getPlayer("name");
                        if(p != null){
                           p.sendMessage(ChatColor.GREEN + KarmicShare.TAG
                                 + " You have been removed from "
                                 + ChatColor.GRAY + group);
                        }
                     }else{
                        sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                              + " Group " + ChatColor.GRAY + group
                              + ChatColor.RED + " does not exist");
                     }
                  }
               }
            }catch(IndexOutOfBoundsException e){
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Player name not given");
               return false;
            }
            return true;
         }
      }
      return false;
   }
}
