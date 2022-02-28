package com.dreamerboy.daggers;

import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;

public class Daggers extends JavaPlugin {
	
	public static Daggers plugin;
	public static Logger log;
	public static DaggersListener daggersListener;
	
	@Override
	public void onEnable() {
		plugin = this;
		log = this.getLogger();
		
		final PluginManager pm = getServer().getPluginManager();
		
		log.info("Registering listeners...");
		daggersListener = new DaggersListener();
		pm.registerEvents(daggersListener, this);
		
		log.info("Registering custom element...");
		new CustomElement();
		
		log.info("Registering abilities...");
		CoreAbility.registerPluginAbilities(this, "com.dreamerboy.daggers.abilities");
		
		log.info("Initializing config...");
		initializeConfig();
		
		log.info("Successfully enabled!");
	}
	
	@Override
	public void onDisable() {
		log.info("Unregistering listeners...");
		HandlerList.unregisterAll(daggersListener);
		
		log.info("Successfully disabled!");
	}
	
	private void initializeConfig() {
		final String path = "ExtraAbilities.DreamerBoy.Chi.DaggerThrow.";
		final FileConfiguration config = ConfigManager.defaultConfig.get();
		
		config.addDefault(path + "Cooldown", 6000);
		config.addDefault(path + "ShotCooldown", 100);
		config.addDefault(path + "Threshold", 500);
		config.addDefault(path + "Speed", 2.5);
		config.addDefault(path + "Damage", 2);
		config.addDefault(path + "MaximumAmount", 6);
		config.addDefault(path + "MaximumHits", 3);
		config.addDefault(path + "RequireArrows.Amount", 1);
		config.addDefault(path + "PreventFusion", false);
		config.addDefault(path + "RemoveSpouts", true);
		config.addDefault(path + "Particles.Enabled", true);
		config.addDefault(path + "Particles.Cosmetics.Enabled", true);
		config.addDefault(path + "Particles.Cosmetics.ChangeAbilities", true);
		config.addDefault(path + "ForceField.Enabled", true);
		config.addDefault(path + "ForceField.Radius", 4);
		config.addDefault(path + "ForceField.Knockback", 3);
		config.addDefault(path + "ForceField.Duration", 1500);
		config.addDefault(path + "Electro.Enabled", true);
		config.addDefault(path + "Electro.StunDuration", 25.0);
		config.addDefault(path + "Electro.Chain.Radius", 4);
		config.addDefault(path + "Electro.Chain.Amount", 3);
		config.addDefault(path + "Poisonous.Enabled", true);
		config.addDefault(path + "Poisonous.Burst.Radius", 5);
		config.addDefault(path + "Poisonous.Poison.Duration", 2);
		config.addDefault(path + "Poisonous.Poison.Amplifier", 1);
		config.addDefault(path + "Explosive.Enabled", true);
		config.addDefault(path + "Explosive.Explosion.Damage", 2);
		config.addDefault(path + "Explosive.Explosion.Power", 2);
		config.addDefault(path + "Explosive.Explosion.BreakBlocks.Enabled", true);
		config.addDefault(path + "Explosive.Explosion.BreakBlocks.PlaceFire", true);
		config.addDefault(path + "Explosive.Explosion.BreakBlocks.RevertExplosion.Enabled", true);
		config.addDefault(path + "Explosive.Explosion.BreakBlocks.RevertExplosion.RevertTime", 4000);
		ConfigManager.defaultConfig.save();
	}
}
