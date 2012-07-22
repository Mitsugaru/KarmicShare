package com.mitsugaru.KarmicShare.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.file.YamlConfiguration;

import com.mitsugaru.KarmicShare.KarmicShare;
import com.mitsugaru.KarmicShare.inventory.Item;

public class KarmaConfig{
   private static KarmicShare plugin;
   public static final Map<Item, KarmaInfo> VALUES = new HashMap<Item, KarmaInfo>();

   public static void init(KarmicShare ks){
      plugin = ks;
      reload();
   }

   /**
    * Reload karma info
    */
   public static void reload(){
      VALUES.clear();
      loadKarmaMap();
   }

   /**
    * Loads the per-item karma values into a hashmap for later usage
    */
   private static void loadKarmaMap(){
      // Load karma file
      final YamlConfiguration karmaFile = karmaFile();
      // Load custom karma file into map
      for(final String entry : karmaFile.getKeys(false)){
         try{
            if(!karmaFile.contains(entry + ".itemid")){
               plugin.getLogger()
                     .warning("Missing item id for entry: " + entry);
               continue;
            }
            final int itemId = karmaFile.getInt(entry + ".itemid");
            int data = 0;
            if(karmaFile.contains(entry + ".data")){
               data = karmaFile.getInt(entry + ".data");
            }
            final int give = karmaFile.getInt(entry + ".give",
                  RootConfig.getInt(ConfigNode.KARMA_CHANGE_GIVE));
            final int take = karmaFile.getInt(entry + ".take",
                  RootConfig.getInt(ConfigNode.KARMA_CHANGE_TAKE));
            if(itemId != 373){
               VALUES.put(new Item(itemId, Byte.parseByte("" + data),
                     (short) data), new KarmaInfo(give, take));
            }else{
               VALUES.put(
                     new Item(itemId, Byte.parseByte("" + 0), (short) data),
                     new KarmaInfo(give, take));
            }
         }catch(final NumberFormatException ex){
            plugin.getLogger().warning("Non-integer value for: " + entry);
            ex.printStackTrace();
         }
      }
      if(RootConfig.getBoolean(ConfigNode.DEBUG_CONFIG)){
         plugin.getLogger().info("Loaded custom karma values");
      }
   }

   /**
    * Loads the karma file. Contains default values If the karma file isn't
    * there, or if its empty, then load defaults.
    * 
    * @return YamlConfiguration file
    */
   private static YamlConfiguration karmaFile(){
      final File file = new File(plugin.getDataFolder().getAbsolutePath()
            + "/karma.yml");
      final YamlConfiguration karmaFile = YamlConfiguration
            .loadConfiguration(file);
      // Insert defaults into config file if they're not present
      if(karmaFile.getKeys(false).isEmpty()){
         // Defaults
         karmaFile.set("Ice.itemid", 79);
         karmaFile.set("Ice.give", 5);
         karmaFile.set("Ice.take", 8);
         try{
            // Save the file
            karmaFile.save(file);
         }catch(IOException e1){
            plugin.getLogger().warning(
                  "File I/O Exception on saving karma list");
            e1.printStackTrace();
         }
      }
      return karmaFile;
   }

}
