package com.mitsugaru.KarmicShare.config;

public class KarmaInfo{
   private int give = 0, take = 0;

   public KarmaInfo(int give, int take){
      this.give = give;
      this.take = take;
   }

   public int getGiveValue(){
      return give;
   }

   public int getTakeValue(){
      return take;
   }
}
