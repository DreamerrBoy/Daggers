package com.dreamerboy.daggers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import com.dreamerboy.daggers.abilities.Dagger;
import com.dreamerboy.daggers.abilities.Dagger.DaggerType;
import com.dreamerboy.daggers.abilities.DaggerThrow;
import com.dreamerboy.daggers.combos.Barrage;
import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.airbending.AirSpout;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ActionBar;
import com.projectkorra.projectkorra.util.ChatUtil;
import com.projectkorra.projectkorra.waterbending.WaterSpout;

import net.md_5.bungee.api.ChatColor;

public class DaggersListener implements Listener {
	
	@EventHandler
	public void onQuit(final PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		
		if(DaggerThrow.DAGGER_TYPES.containsKey(player.getUniqueId()))
			DaggerThrow.DAGGER_TYPES.remove(player.getUniqueId());
		
		if(DaggerThrow.COSMETICS.containsKey(player.getUniqueId()))
			DaggerThrow.COSMETICS.remove(player.getUniqueId());
	}
	
	@EventHandler
	public void onInteract(final PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		
		if(player == null || bPlayer == null || !event.getAction().equals(Action.LEFT_CLICK_AIR) && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			return;
		}
		
		if(bPlayer.getBoundAbilityName().equalsIgnoreCase("DaggerThrow")) {
			new DaggerThrow(player);
		}
	}
	
	@EventHandler
	public void onChat(final AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();
		final String msg = event.getMessage();
		
		final String path = "ExtraAbilities.DreamerBoy.Chi.DaggerThrow.";
		final FileConfiguration config = ConfigManager.defaultConfig.get();
		
		if(!config.getBoolean(path + "Particles.Enabled") || !config.getBoolean(path + "Particles.Cosmetics.Enabled"))
			return;
		
		if(!player.hasPermission("bending.ability.daggerthrow.cosmetics"))
			return;
		
		if(msg.startsWith("<daggers>")) {
			final String colorcode = msg.split(">")[1];
			final String[] color = colorcode.split(", ");
			if(color.length == 3) {
				if(isParsable(color[0]) && isParsable(color[1]) && isParsable(color[2])) {
					final int r = Integer.parseInt(color[0]);
					final int g = Integer.parseInt(color[1]);
					final int b = Integer.parseInt(color[2]);
					
					if(r <= 255 && g <= 255 && b <= 255) {
						final Color c = Color.fromRGB(r, g, b);
						DaggerThrow.COSMETICS.put(player.getUniqueId(), c);
						ChatUtil.sendBrandingMessage(player, CustomElement.DAGGERS.getColor() + "You have successfully changed your daggers trail!");
						event.setCancelled(true);
					}
				}
			} else if(colorcode.equalsIgnoreCase("off")) {
				if(DaggerThrow.COSMETICS.containsKey(player.getUniqueId()))
					DaggerThrow.COSMETICS.remove(player.getUniqueId());
				ChatUtil.sendBrandingMessage(player, CustomElement.DAGGERS.getColor() + "You have successfully reset your daggers trail!");
				event.setCancelled(true);
			}
		}
	}
	
	private boolean isParsable(final String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}
	
	@EventHandler
	public void onSneak(final PlayerToggleSneakEvent e) {
		final Player player = e.getPlayer();
		final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
		
		if(e.isCancelled() || bPlayer == null || e.isSneaking() || player.getGameMode().equals(GameMode.SPECTATOR))
			return;
		
		if(bPlayer.getBoundAbilityName().equalsIgnoreCase("DaggerThrow")) {
			final DaggerType current = DaggerThrow.DAGGER_TYPES.getOrDefault(player.getUniqueId(), DaggerType.NO_EFFECT);
			final List<DaggerType> list = new ArrayList<>(Arrays.asList(DaggerType.values()).stream().filter(type -> type.isEnabled() && player.hasPermission("bending.ability.daggerthrow." + type.toString())).collect(Collectors.toList()));
			
			if(list.size() <= 1) 
				return;
				
			DaggerType newType;
			if(list.indexOf(current)+1 > list.size()-1)
				newType = list.get(0);
			else
				newType = list.get(list.indexOf(current)+1);
			
			DaggerThrow.DAGGER_TYPES.put(player.getUniqueId(), newType);
			ActionBar.sendActionBar("" + CustomElement.DAGGERS.getColor() + ChatColor.BOLD + (bPlayer.isOnCooldown("DaggerThrow") ? ChatColor.STRIKETHROUGH : "") + newType.getDisplay(), player);
		}
	}
	
	@EventHandler
	public void onProjectileHit(final ProjectileHitEvent event) {
		if(event.getHitEntity() != null || event.getHitBlock() == null)
			return;
		
		if(event.getEntity() instanceof Arrow && event.getEntity().getShooter() instanceof Player) {
			final Arrow arrow = (Arrow) event.getEntity();
			
			final Dagger dagger = Dagger.getAbilityFromArrow(arrow);
			
			if(dagger != null) {
				if(dagger.getType().equals(DaggerType.EXPLOSIVE))
					dagger.createExplosion(event.getHitBlock().getLocation());
				else if(dagger.getType().equals(DaggerType.POISONOUS))
					dagger.createPoisonBurst(event.getHitBlock().getLocation());
				
				dagger.remove();
			} else {
				final Barrage barrage = Barrage.getAbilityFromArrow(arrow);
				if(barrage != null) {
					barrage.getDaggerFromArrow(arrow).remove();
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Arrow) {
			final Arrow arrow = (Arrow) event.getDamager();
			
			if(arrow.getShooter() instanceof Player) {
				final Dagger dagger = Dagger.getAbilityFromArrow(arrow);
				final Barrage barrage = Barrage.getAbilityFromArrow(arrow);
				
				if(dagger != null) {
					event.setDamage(0D);
					event.setCancelled(true);
					dagger.damageEntity(event.getEntity());
					dagger.remove();
					if(DaggerThrow.spouts && event.getEntity() instanceof Player) {
						final Player target = (Player) event.getEntity();
						final AirSpout airSpout = CoreAbility.getAbility(target, AirSpout.class);
						final WaterSpout waterSpout = CoreAbility.getAbility(target, WaterSpout.class);
						
						if(airSpout != null)
							airSpout.remove();
						
						if(waterSpout != null)
							waterSpout.remove();
					}
				} else if(barrage != null) {
					event.setDamage(0D);
					event.setCancelled(true);
					barrage.damageEntity(event.getEntity());
					barrage.getDaggerFromArrow(arrow).remove();
					if(DaggerThrow.spouts && event.getEntity() instanceof Player) {
						final Player target = (Player) event.getEntity();
						final AirSpout airSpout = CoreAbility.getAbility(target, AirSpout.class);
						final WaterSpout waterSpout = CoreAbility.getAbility(target, WaterSpout.class);
						
						if(airSpout != null)
							airSpout.remove();
						
						if(waterSpout != null)
							waterSpout.remove();
					}
				}
			}		
		}
	}
}
