package net.z0ttel.granulargrief;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import net.minecraft.launchwrapper.Launch;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import java.io.File;
import java.util.Set;
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

import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import net.minecraftforge.common.config.Configuration;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.ai.EntityAITasks;


//@Mod(modid = GranularGrief.MODID, name = GranularGrief.NAME, version = GranularGrief.VERSION, serverSideOnly = true, acceptableRemoteVersions = "*")
@Mod(modid = GranularGrief.MODID, name = GranularGrief.NAME, version = GranularGrief.VERSION, acceptableRemoteVersions = "*")
public class GranularGrief
{
	public static final String MODID = "granulargrief";
	public static final String NAME = "Granular Grief";
	public static final String VERSION = "1.10.2-0.1";
	
	public static Logger logger;
	
	private boolean enderGrief = true;
	private boolean creeperGrief = true;
	
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		
		// Config
		Configuration configFile = new Configuration(event.getSuggestedConfigurationFile());
		enderGrief = configFile.getBoolean("enderGrief", Configuration.CATEGORY_GENERAL, enderGrief, "Shall Endermen tear up your surroundings?");
		creeperGrief = configFile.getBoolean("creeperGrief", Configuration.CATEGORY_GENERAL, creeperGrief, "Do you want creeper holes?");
		if(configFile.hasChanged()) configFile.save();
		
		
		if(!enderGrief) {
			//logger.info("Registering event handler to remove enderman griefing AI.");
			//MinecraftForge.EVENT_BUS.register(new EndermanHandler());
			
			logger.info("granulargrief: Emptying the global EntityEnderman.CARRIABLE_BLOCKS.");
			// Use reflection to empty the global EntityEnderman.CARRIABLE_BLOCKS set.
			try {
				String fName = ((Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment")) ? "CARRIABLE_BLOCKS" : "field_70827_d";
				Field f = EntityEnderman.class.getDeclaredField(fName); //NoSuchFieldException
				f.setAccessible(true);
				Set<Block> carriable = (Set<Block>) f.get(null); //IllegalAccessException
				carriable.clear();
			} catch(Exception e) {
				logger.info("granulargrief: ERROR: Could not access EntityEnderman.CARRIABLE_BLOCKS: " + e);
			}
			
		}
		
		if(!creeperGrief) {
			logger.info("granulargrief: Registering event handler to remove blocks destruction from creeper detonations.");
			MinecraftForge.EVENT_BUS.register(new CreeperHandler());
		}
	}
	
	public class CreeperHandler {
		@SubscribeEvent
		public void onDetonate(ExplosionEvent.Detonate event) {
			// TODO: Add filter for specific blocks.
			if(event.getExplosion().getExplosivePlacedBy() instanceof EntityCreeper) {
				event.getAffectedBlocks().clear();
			}
		}
	}
	/*
	public class EndermanHandler {
		@SubscribeEvent
		public void onJoinWorld(EntityJoinWorldEvent event) {
			Entity entity = event.getEntity();
			if(entity instanceof EntityEnderman) {
				//logger.info("granulargrief: Entity " + entity.getClass().getName() + " joined the world.");
				EntityEnderman enderman = (EntityEnderman) entity;
				
				List<EntityAITasks.EntityAITaskEntry> toRemove = new ArrayList<EntityAITasks.EntityAITaskEntry>();
				
				for(EntityAITasks.EntityAITaskEntry taskEntry: enderman.tasks.taskEntries) {
					//logger.info("granulargrief: Entity " + enderman.getClass().getName() + " has AI Task " + taskEntry.action.getClass().getName());
					// Funny workaround for class obfuscation
					// Remove all tasks that are a subclass of the enderman class
					// So far this should only remove the enderman griefing.
					if(taskEntry.action.getClass().getName().startsWith(enderman.getClass().getName()))
					{
						toRemove.add(taskEntry);
					}
				}
				for(EntityAITasks.EntityAITaskEntry entry: toRemove) {
						//logger.info("granulargrief: Entity " + enderman.getClass().getName() + " has AI Task " + entry.action.getClass().getName() + " that is now removed.");
						enderman.tasks.removeTask(entry.action);
				}
				
			}
		}
	}
	*/
}

