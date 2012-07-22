/**
 * Separate class to handle commands Followed example from DiddiZ's LB.
 * 
 * @author Mitsugaru
 */
package com.mitsugaru.KarmicShare.commands;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.ChatPaginator;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.config.RootConfig;
import com.mitsugaru.KarmicShare.config.ConfigNode;
import com.mitsugaru.KarmicShare.logic.Karma;
import com.mitsugaru.KarmicShare.logic.Karma.Direction;
import com.mitsugaru.KarmicShare.permissions.PermissionHandler;
import com.mitsugaru.KarmicShare.permissions.PermissionNode;

public class Commander implements CommandExecutor{
   // Class variables
   private final KarmicShare plugin;
   private final static String bar = "======================";
   private long time = 0;

   /**
    * Constructor
    * 
    * @param karmicShare
    *           plugin
    */
   public Commander(KarmicShare plugin){
      // Instantiate variables
      this.plugin = plugin;
      // Initialize commands classes
      ItemCommands.init(plugin);
      ListCommands.init(plugin);
      GroupCommands.init(plugin);
      AdminCommands.init(plugin);
   }

   /**
    * Command handler
    */
   @Override
   public boolean onCommand(CommandSender sender, Command cmd,
         String commandLabel, String[] args){
      if(RootConfig.getBoolean(ConfigNode.DEBUG_TIME)){
         time = System.nanoTime();
      }
      // See if any arguments were given
      if(args.length == 0){
         noArgs(sender);
      }else{
         // Grab command
         final String com = args[0].toLowerCase();
         if(com.equals("version") || com.equals("ver")){
            // Version and author
            this.showVersion(sender, args);
         }else if(com.equals("?") || com.equals("help")){
            // Help
            this.displayHelp(sender);
         }else if(com.equals("info")){
            // Info command
            ItemCommands.inspectItem(sender, args);
         }else if(com.equals("give")){
            // Player is giving item to pool
            ItemCommands.giveItem(sender, args);
         }else if(com.equals("take")){
            // Player requested an item
            ItemCommands.takeItem(sender, args);
         }else if(com.equals("prev")){
            // Previous page of item pool
            if(PermissionHandler.has(sender, PermissionNode.COMMANDS_LIST)){
               // List, with previous page
               ListCommands.listPool(sender, -1);
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.COMMANDS_LIST.getNode());
            }
         }else if(com.equals("next")){
            // Next page of item pool
            if(PermissionHandler.has(sender, PermissionNode.COMMANDS_LIST)){
               // List with next page
               ListCommands.listPool(sender, 1);
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.COMMANDS_LIST.getNode());
            }
         }else if(com.equals("list")){
            // List items in pool
            if(PermissionHandler.has(sender, PermissionNode.COMMANDS_LIST)){
               ListCommands.listCommand(sender, args);
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.COMMANDS_LIST.getNode());
            }
         }else if(com.equals("value")){
            // Ask for karma multipliers / page through muliplier list
            if(PermissionHandler.has(sender, PermissionNode.COMMANDS_VALUE)){
               ListCommands.valueCommand(sender, args);
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Lack permission: "
                     + PermissionNode.COMMANDS_VALUE.getNode());
            }
         }else if(com.equals("page")){
            if(plugin.useChest()){
               if(PermissionHandler.has(sender, PermissionNode.CHEST)){
                  if(args.length > 1){
                     try{
                        Integer page = Integer.parseInt(args[1]);
                        Karma.chestPage.put(sender.getName(), page);
                        sender.sendMessage(ChatColor.GREEN + KarmicShare.TAG
                              + " Right click on sign to jump to page "
                              + ChatColor.GOLD + page.intValue());
                     }catch(NumberFormatException e){
                        sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                              + " Invalid number: " + ChatColor.GOLD + args[1]);
                     }
                  }
               }else{
                  sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                        + " Lack permission: " + PermissionNode.CHEST.getNode());
               }
            }else{
               sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                     + " Chests are disabled.");
            }
         }else if(com.equals("open")){
            openCommand(sender, args);
         }else if(com.equals("group")){
            // Group command
            GroupCommands.parseCommand(sender, args);
         }else if(com.equals("admin")){
            // Admin command
            AdminCommands.parseCommand(sender, args);
         }else if(com.equals("player")){
            // Other player karma lookup
            this.otherPlayerKarma(sender, args);
         }else{
            // Bad command entered
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Wrong syntax. Try /ks ? for help.");
         }
      }
      if(RootConfig.getBoolean(ConfigNode.DEBUG_TIME)){
         time = System.nanoTime() - time;
         sender.sendMessage("[Debug]" + KarmicShare.TAG + "Process time: "
               + time);
      }
      return true;
   }

   private void noArgs(CommandSender sender){
      // Check if they have "karma" permission
      if(!PermissionHandler.has(sender, PermissionNode.KARMA)){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " Lack permission: " + PermissionNode.KARMA.getNode());
         return;
      }
      if(RootConfig.getBoolean(ConfigNode.KARMA_DISABLED)){

         // karma system disabled
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Karma disabled");
         return;
      } else if(RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY)){
         sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
               + " Uses economy system");
         return;
      }
      if(!(sender instanceof Player)){
         // Show help to console
         displayHelp(sender);
         return;
      }
      // Show player karma
      this.showPlayerKarma(sender);
      // Show groups
      String current = Karma.selectedGroup.get(sender.getName());
      if(current == null){
         Karma.selectedGroup.put(sender.getName(), "global");
         current = "global";
      }
      final StringBuilder sb = new StringBuilder();
      for(String s : Karma.getPlayerGroups(sender, sender.getName())){
         if(s.equalsIgnoreCase(current)){
            sb.append(ChatColor.BOLD + "" + ChatColor.LIGHT_PURPLE + s
                  + ChatColor.RESET + " ");
         }else{
            sb.append(ChatColor.GRAY + s + " ");
         }
      }
      // Remove trailing characters
      try{
         sb.deleteCharAt(sb.length() - 1);
         final String[] out = ChatPaginator.wordWrap(ChatColor.YELLOW
               + KarmicShare.TAG + " Groups: " + sb.toString(),
               ChatPaginator.AVERAGE_CHAT_PAGE_WIDTH - 1);
         for(String line : out){
            sender.sendMessage(line);
         }
      }catch(StringIndexOutOfBoundsException e){
         sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG + " No groups");
      }
   }

   private void openCommand(CommandSender sender, String[] args){
      if(!plugin.useChest()){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " Chests are disabled.");
         return;
      }else if(!(sender instanceof Player)){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " Cannot use this command as console.");
         return;
      }else if(!PermissionHandler.has(sender, PermissionNode.COMMANDS_OPEN)){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " Lack permission: " + PermissionNode.COMMANDS_OPEN.getNode());
         return;
      }
      final Player player = (Player) sender;

      // Check if in valid world
      if(player.getWorld() == null){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " Something went wrong: World was null?");
         return;
      }
      final String world = player.getWorld().getName();
      if(RootConfig.getStringList(ConfigNode.DISABLED_WORLDS).contains(world)){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " KarmicShare access disabled for this world.");
         return;
      }
      // Grab current group
      String current = Karma.selectedGroup.get(sender.getName());
      if(current == null){
         Karma.selectedGroup.put(sender.getName(), "global");
         current = "global";
      }
      // Validate group
      if(!Karma.validGroup(sender, current)){
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Group "
               + ChatColor.GRAY + current + ChatColor.RED + " does not exist");
         return;
      }
      // check if page is given, else default to 1
      int page = 1;
      if(args.length > 1){
         try{
            page = Integer.parseInt(args[1]);
         }catch(NumberFormatException e){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Invalid number: " + ChatColor.GOLD + args[1]);
            page = 1;
         }
      }
      // Get valid page
      page = Karma.grabPage(page, current, Direction.CURRENT);
      // Show inventory
      Karma.showInventory(player, current, page);
   }

   private void otherPlayerKarma(CommandSender sender, String[] args){
      // Check if karma is enabled
      if(RootConfig.getBoolean(ConfigNode.KARMA_DISABLED)){
         // karma system disabled
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG + " Karma disabled");
         return;
      }else if(RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY)){
         sender.sendMessage(ChatColor.YELLOW + KarmicShare.TAG
               + " Uses economy system");
         return;
      }
      // Check if name was given
      if(args.length <= 1){
         // did not give a player name, therefore error
         sender.sendMessage(ChatColor.RED + KarmicShare.TAG
               + " No player name given.");
         return;
      }
      // Check if they have the permission node
      if(PermissionHandler.has(sender, "KarmicShare.admin")
            || PermissionHandler.has(sender, "KarmicShare.karma.other")){
         // attempt to parse name
         String name = plugin.expandName(args[1]);
         if(name == null){
            name = args[1];
         }
         try{
            // Colorize karma
            sender.sendMessage(this.colorizeKarma(Karma.getPlayerKarma(name)));
         }catch(SQLException e){
            sender.sendMessage(ChatColor.RED + KarmicShare.TAG
                  + " Could not get " + name + "'s karma");
            e.printStackTrace();
         }
      }else{
         sender.sendMessage(ChatColor.RED
               + " Lack permission: KarmicShare.karma.other");
      }
   }

   private void showVersion(CommandSender sender, String[] args){
      sender.sendMessage(ChatColor.BLUE + bar + "=====");
      sender.sendMessage(ChatColor.GREEN + "KarmicShare v"
            + plugin.getDescription().getVersion());
      sender.sendMessage(ChatColor.GREEN + "Coded by Mitsugaru");
      sender.sendMessage(ChatColor.WHITE + "Shout outs: " + ChatColor.GOLD
            + "@khanjal");
      sender.sendMessage(ChatColor.BLUE + "===========" + ChatColor.GRAY
            + "Config" + ChatColor.BLUE + "===========");
      sender.sendMessage(ChatColor.GRAY + "Effects: "
            + RootConfig.getBoolean(ConfigNode.EFFECTS));
      sender.sendMessage(ChatColor.GRAY + "Chests: "
            + RootConfig.getBoolean(ConfigNode.CHESTS));
      sender.sendMessage(ChatColor.GRAY + "Karma enabled: "
            + !RootConfig.getBoolean(ConfigNode.KARMA_DISABLED));
      sender.sendMessage(ChatColor.GRAY + "Static karma: "
            + RootConfig.getBoolean(ConfigNode.KARMA_STATIC));
      sender.sendMessage(ChatColor.GRAY + "Use Economy: "
            + RootConfig.getBoolean(ConfigNode.KARMA_ECONOMY));
      sender.sendMessage(ChatColor.GRAY + "Karma lower-upper limit: "
            + RootConfig.getInt(ConfigNode.KARMA_LOWER_LIMIT) + " :: "
            + RootConfig.getInt(ConfigNode.KARMA_UPPER_LIMIT));
      sender.sendMessage(ChatColor.GRAY + "Karma lower/upper %: "
            + RootConfig.getDouble(ConfigNode.KARMA_LOWER_PERCENT) * 100
            + "% / " + RootConfig.getDouble(ConfigNode.KARMA_UPPER_PERCENT)
            * 100 + "%");
      sender.sendMessage(ChatColor.GRAY + "Default karma: "
            + RootConfig.getInt(ConfigNode.KARMA_PLAYER_DEFAULT));
      sender.sendMessage(ChatColor.GRAY + "Default karma rate: "
            + RootConfig.getInt(ConfigNode.KARMA_CHANGE_GIVE) + " : "
            + RootConfig.getInt(ConfigNode.KARMA_CHANGE_TAKE));
   }

   private void showPlayerKarma(CommandSender sender){
      // Check if player sent command
      if(!(sender instanceof Player)){
         return;
      }
      Player player = (Player) sender;
      try{
         // Retrieve karma from database and colorize
         sender.sendMessage(this.colorizeKarma(Karma.getPlayerKarma(player
               .getName())));
      }catch(SQLException e){
         player.sendMessage(ChatColor.RED + KarmicShare.TAG
               + "Could not obtain player karma!");
         e.printStackTrace();
      }
   }

   /**
    * Show the help menu, with commands and description
    * 
    * @param sender
    *           to display to
    */
   private void displayHelp(CommandSender sender){
      sender.sendMessage(ChatColor.BLUE + "=====" + ChatColor.RED
            + "KarmicShare" + ChatColor.BLUE + "=====");
      sender.sendMessage(ChatColor.GREEN + "/ks" + ChatColor.YELLOW
            + " : Show karma");
      if(PermissionHandler.has(sender, PermissionNode.GIVE)){
         sender.sendMessage(ChatColor.GREEN + "/ks give" + ChatColor.YELLOW
               + " : Give item stack in current hand");
      }
      if(PermissionHandler.has(sender, PermissionNode.TAKE)){
         sender.sendMessage(ChatColor.GREEN + "/ks take <item>[:data] [amount]"
               + ChatColor.YELLOW + " : Take item(s) from pool");
         sender.sendMessage(ChatColor.GREEN + "/ks take <item name> [amount]"
               + ChatColor.YELLOW + " : Take item(s) from pool");
      }
      if(PermissionHandler.has(sender, PermissionNode.COMMANDS_LIST)){
         sender.sendMessage(ChatColor.GREEN + "/ks list [page]"
               + ChatColor.YELLOW + " : List items in pool");
         sender.sendMessage(ChatColor.GREEN + "/ks <prev | next>"
               + ChatColor.YELLOW + " : Show previous/next page of list");
      }
      if(PermissionHandler.has(sender, PermissionNode.COMMANDS_VALUE)){
         sender.sendMessage(ChatColor.GREEN + "/ks value [prev|next|page#]"
               + ChatColor.YELLOW + " : List karma multiplier values");
      }
      if(plugin.useChest()
            && PermissionHandler.has(sender, PermissionNode.CHEST)){
         sender.sendMessage(ChatColor.GREEN + "/ks page <num>"
               + ChatColor.YELLOW + " : Jump page numbers for chests");
         sender.sendMessage(ChatColor.GREEN + "/ks open [page]"
               + ChatColor.YELLOW + " : Open inventory view");
      }
      if(PermissionHandler.has(sender, PermissionNode.GROUP_CREATE)
            || PermissionHandler.has(sender, PermissionNode.GROUP_ADD)
            || PermissionHandler.has(sender, PermissionNode.GROUP_REMOVE)
            || PermissionHandler.has(sender, PermissionNode.GROUP_LEAVE)){
         sender.sendMessage(ChatColor.GREEN + "/ks group" + ChatColor.YELLOW
               + " : List group commands");
      }
      if(PermissionHandler.has(sender, PermissionNode.INFO)){
         sender.sendMessage(ChatColor.GREEN + "/ks info" + ChatColor.YELLOW
               + " : Inspect currently held item");
      }
      if(PermissionHandler.has(sender, PermissionNode.KARMA_OTHER)){
         sender.sendMessage(ChatColor.GREEN + "/ks player <name>"
               + ChatColor.YELLOW + " : Show karma for given player name");
      }
      if(PermissionHandler.has(sender, PermissionNode.ADMIN_ADD)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_RESET)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_SET)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_DRAIN)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_RELOAD)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_ADD)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_CREATE)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_DELETE)
            || PermissionHandler.has(sender, PermissionNode.ADMIN_GROUP_REMOVE)){
         sender.sendMessage(ChatColor.GREEN + "/ks admin" + ChatColor.YELLOW
               + " : List admin commands");
      }
      sender.sendMessage(ChatColor.GREEN + "/ks help" + ChatColor.YELLOW
            + " : Show help menu");
      sender.sendMessage(ChatColor.GREEN + "/ks version" + ChatColor.YELLOW
            + " : Show version and config");
   }

   /**
    * Colorizes the karma based on percentages in the config file
    * 
    * @param player
    *           karma
    * @return Appropriate string with color codes
    */
   private String colorizeKarma(int karma){
      // Colorize based on how high/low karma is
      if(Math.abs(karma + RootConfig.getInt(ConfigNode.KARMA_LOWER_LIMIT)) <= Math
            .abs(karma + RootConfig.getInt(ConfigNode.KARMA_UPPER_LIMIT))){
         // Positive karma
         if(((double) karma + Math.abs(RootConfig
               .getInt(ConfigNode.KARMA_LOWER_LIMIT)))
               / ((double) Math.abs(RootConfig
                     .getInt(ConfigNode.KARMA_UPPER_LIMIT)) + Math
                     .abs(RootConfig.getInt(ConfigNode.KARMA_LOWER_LIMIT))) >= RootConfig
                  .getDouble(ConfigNode.KARMA_UPPER_PERCENT)){
            return (ChatColor.YELLOW + KarmicShare.TAG + ChatColor.GREEN
                  + " Karma: " + karma);
         }else{
            // Not in upper percentage
            return (ChatColor.YELLOW + KarmicShare.TAG + " Karma: " + karma);
         }
      }else{
         // Negative karma
         if(((double) karma + Math.abs(RootConfig
               .getInt(ConfigNode.KARMA_LOWER_LIMIT)))
               / ((double) Math.abs(RootConfig
                     .getInt(ConfigNode.KARMA_UPPER_LIMIT)) + Math
                     .abs(RootConfig.getInt(ConfigNode.KARMA_LOWER_LIMIT))) <= RootConfig
                  .getDouble(ConfigNode.KARMA_LOWER_PERCENT)){
            return (ChatColor.YELLOW + KarmicShare.TAG + ChatColor.RED
                  + " Karma: " + karma);
         }else{
            // Not in lower percentage
            return (ChatColor.YELLOW + KarmicShare.TAG + " Karma: " + karma);
         }
      }
   }
}