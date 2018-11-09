package net.z0ttel.granulargrief;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Field;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import net.minecraftforge.common.config.Configuration;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.ai.EntityAITasks;


@Mod(modid = GranularGrief.MODID, name = GranularGrief.NAME, version = GranularGrief.VERSION, acceptableRemoteVersions = "*")
public class GranularGrief
{
	public static final String MODID = "granulargrief";
	public static final String NAME = "Granular Grief";
	public static final String VERSION = "1.10.2-0.1";
	
	public static Logger logger;
	
	private boolean enderGrief = false;
	private String enderWhitelist[] = {
		"minecraft:anvil",
		"minecraft:cake",
		"minecraft:fence",
		"minecraft:tnt",
	};
	Set<Block> enderBlockWhitelist = new HashSet<Block>();
	
	private boolean creeperGrief = false;
	private String creeperWhitelist[] = {
		"minecraft:fence",
		"minecraft:tnt",
		"minecraft:torch",
		"minecraft:wooden_door",
	};
	Set<Block> creeperBlockWhitelist = new HashSet<Block>();
	
	
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		
		// Config
		Configuration configFile = new Configuration(event.getSuggestedConfigurationFile());

		enderGrief = configFile.getBoolean("enderGrief", Configuration.CATEGORY_GENERAL, enderGrief, "Shall Endermen tear up your surroundings?");
		enderWhitelist =  configFile.getStringList("enderWhitelist", Configuration.CATEGORY_GENERAL, enderWhitelist, "Blocks that are allowed to be griefed by endermen even when enderGrief is false.");

		creeperGrief = configFile.getBoolean("creeperGrief", Configuration.CATEGORY_GENERAL, creeperGrief, "Do you want creeper holes?");
		creeperWhitelist =  configFile.getStringList("creeperWhitelist", Configuration.CATEGORY_GENERAL, creeperWhitelist, "Blocks that are allowed to be griefed by creepers even when creeperGrief is false.");
		
		for(String id: enderWhitelist) {
			ResourceLocation resId = new ResourceLocation(id);
			if(ForgeRegistries.BLOCKS.containsKey(resId)) {
				logger.info("granulargrief: Endermen: Whitelisting '" + id + "'.");
				enderBlockWhitelist.add(ForgeRegistries.BLOCKS.getValue(resId));
			} else {
				logger.info("granulargrief: Could not find in block registry: " + id);
			}
		}
		
		for(String id: creeperWhitelist) {
			ResourceLocation resId = new ResourceLocation(id);
			if(ForgeRegistries.BLOCKS.containsKey(resId)) {
				logger.info("granulargrief: Creeper: Whitelisting '" + id + "'.");
				creeperBlockWhitelist.add(ForgeRegistries.BLOCKS.getValue(resId));
			} else {
				logger.info("granulargrief: Could not find in block registry: " + id);
			}
		}
		
		/*
		for(ResourceLocation key: ForgeRegistries.BLOCKS.getKeys()) {
			logger.info("granulargrief: Block key in ForgeRegistries: " + key);
		}
		*/
		
		if(configFile.hasChanged()) configFile.save();
		
		if(!enderGrief) {
			logger.info("granulargrief: Configuring the global EntityEnderman.CARRIABLE_BLOCKS.");
			// Use reflection to empty the global EntityEnderman.CARRIABLE_BLOCKS set.
			try {
				String fName = ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) ? "CARRIABLE_BLOCKS" : "field_70827_d";
				Field f = EntityEnderman.class.getDeclaredField(fName); //NoSuchFieldException
				f.setAccessible(true);
				Set<Block> carriable = (Set<Block>) f.get(null); //IllegalAccessException
				carriable.clear();
				carriable.addAll(enderBlockWhitelist);
			} catch(Exception e) {
				logger.info("granulargrief: ERROR: Could not access EntityEnderman.CARRIABLE_BLOCKS: " + e);
			}
			
		}
		
		if(!creeperGrief) {
			logger.info("granulargrief: Registering event handler to configure block destruction from creeper detonations.");
			MinecraftForge.EVENT_BUS.register(new CreeperHandler());
		}
	}
	
	public class CreeperHandler {
		@SubscribeEvent
		public void onDetonate(ExplosionEvent.Detonate event) {
			// TODO: Add filter for specific blocks.
			if(event.getExplosion().getExplosivePlacedBy() instanceof EntityCreeper) {
				// without whitelist
				//event.getAffectedBlocks().clear();
				
				// Using an iterator allows me to remove while iterating.
				Iterator<BlockPos> iter = event.getAffectedBlocks().iterator();
				while (iter.hasNext()) {
					BlockPos pos = iter.next();
					IBlockState blockstate = event.getWorld().getBlockState(pos);
					if (!creeperBlockWhitelist.contains(blockstate.getBlock())) {
						iter.remove();
					}
				}
			}
		}
	}
}

