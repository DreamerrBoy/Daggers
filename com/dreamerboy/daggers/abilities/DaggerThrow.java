package com.dreamerboy.daggers.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.dreamerboy.daggers.CustomElement;
import com.dreamerboy.daggers.DaggersAbility;
import com.dreamerboy.daggers.abilities.Dagger.DaggerType;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;

public class DaggerThrow extends DaggersAbility {
	
	public static final Map<UUID, Color> COSMETICS = new ConcurrentHashMap<>();
	public static final Map<UUID, DaggerType> DAGGER_TYPES = new ConcurrentHashMap<>();
	
	private long cooldown, lastArrow, shotCooldown, threshold;
	private double speed;
	private int maxAmount, amount, maxHits, requiredArrows;
	private Map<Integer, Integer> hitCount = new HashMap<>();
	
	private boolean particles, cosmetics, limit;
	
	public static boolean poisonous = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.Poisonous.Enabled"),
			  explosive = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.Explosive.Enabled"),
			  forcefield = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.ForceField.Enabled"),
			  electro = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.Electro.Enabled"),
			  fusion = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.PreventFusion"),
			  spouts = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.RemoveSpouts"),
			  changeAbilities = ConfigManager.getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.Particles.Cosmetics.ChangeAbilities");
	
	private List<Dagger> daggers = new ArrayList<>();
	
	public DaggerThrow(final Player player) {
		super(player);
	
		this.requiredArrows = ConfigManager.getConfig().getInt("ExtraAbilities.DreamerBoy.Chi.DaggerThrow.RequireArrows.Amount");
		
		if(!bPlayer.canBend(this) || (this.doesRequireArrow() && !player.getInventory().contains(Material.ARROW, this.requiredArrows)))
			return;
		
		final DaggerThrow daggerThrow = CoreAbility.getAbility(player, DaggerThrow.class);
		if(daggerThrow != null) {
			daggerThrow.shootDagger(DAGGER_TYPES.getOrDefault(player.getUniqueId(), DaggerType.NO_EFFECT));
			return;
		}
		
		setFields();
		start();
		
		shootDagger(DAGGER_TYPES.getOrDefault(player.getUniqueId(), DaggerType.NO_EFFECT));
	}
	
	private void setFields() {
		final String path = "ExtraAbilities.DreamerBoy.Chi.DaggerThrow.";
		
		this.cooldown = ConfigManager.getConfig().getLong(path + "Cooldown");
		this.shotCooldown = ConfigManager.getConfig().getLong(path + "ShotCooldown");
		this.threshold = ConfigManager.getConfig().getLong(path + "Threshold");
		this.speed = ConfigManager.getConfig().getDouble(path + "Speed");
		this.maxAmount = ConfigManager.getConfig().getInt(path + "MaximumAmount");
		this.maxHits = ConfigManager.getConfig().getInt(path + "MaximumHits");
		this.amount = this.maxAmount;
		this.limit = amount > 0;
	
		this.particles = ConfigManager.getConfig().getBoolean(path + "Particles.Enabled");
		this.cosmetics = ConfigManager.getConfig().getBoolean(path + "Particles.Cosmetics.Enabled");
	}

	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public Location getLocation() {
		return null;
	}
	
	@Override
	public List<Location> getLocations() {
		return this.daggers.stream().map(Dagger::getArrow).map(Arrow::getLocation).collect(Collectors.toList());
	}

	@Override
	public String getName() {
		return "DaggerThrow";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return true;
	}
	
	@Override
	public String getDescription() {
		final ChatColor chi = Element.CHI.getColor();
		final ChatColor sub = CustomElement.DAGGERS.getColor();
		final String l = "" + ChatColor.DARK_GRAY + "- ";
		
		return "This ability allows you to throw sharp daggers towards your enemies.\n" +
				"There are " + chi + "5 different dagger types" + sub + ";\n" + 
				l + chi + "Normal Daggers\n" + 
				l + chi + "Poisonous Daggers" + ChatColor.DARK_GRAY + ": " + sub + "These daggers give your enemies poison effect. Also they create a poison burst when they hit a block.\n" + 
				l + chi + "Explosive Daggers" + ChatColor.DARK_GRAY + ": " + sub + "These daggers explode where they hit.\n" + 
				l + chi + "ForceField Daggers" + ChatColor.DARK_GRAY + ": " + sub + "These daggers ignore gravity and they push away your opponents while moving.\n" +
				l + chi + "Electro Daggers" + ChatColor.DARK_GRAY + ": " + sub + "These daggers electrocute your enemies. In addition to this, they create a chain effect around the hit entity.";
	}
	
	@Override
	public String getInstructions() {
		return "Left click multiple times to throw daggers.\n"
				+ "Tap sneak to switch between dagger types.";
	}
	
	@Override
	public void progress() {
		if(!bPlayer.canBend(this) || (System.currentTimeMillis() > lastArrow + threshold)) {
			remove();
			return;
		}
	}
	
	@Override
	public void remove() {
		bPlayer.addCooldown(this);
		super.remove();
	}
	
	public boolean shootDagger(final DaggerType type) {
		if(bPlayer.isOnCooldown("DaggerThrowShot") || (this.limit && --this.amount < 0) || (this.doesRequireArrow() && !removeItem(player, Material.ARROW, 1)))
			return false;
		
		final Dagger dagger = new Dagger(this, type);
		final Arrow arrow = dagger.getArrow();
		
		final Entity targetedEntity = GeneralMethods.getTargetedEntity(this.player, this.speed*4);
		Location targetedLocation = GeneralMethods.getTargetedLocation(this.player, this.speed*4);
		if(targetedEntity != null)
			targetedLocation = targetedEntity.getLocation().add(0, 1, 0);
		targetedLocation.setPitch(0);
		targetedLocation.setYaw(0);
		
		final Vector vector = targetedLocation.toVector().subtract(this.player.getEyeLocation().toVector());
		arrow.setVelocity(vector.normalize().multiply(this.speed));
		
		this.lastArrow = System.currentTimeMillis();
		this.daggers.add(dagger);
		this.bPlayer.addCooldown("DaggerThrowShot", shotCooldown);
		player.getLocation().getWorld().playSound(player.getLocation().add(0,2,0), Sound.ITEM_TRIDENT_THROW, 1f, 1.3f);
		return true;
	}

	public static DaggerThrow getAbilityFromArrow(final Arrow arrow) {
		for(final DaggerThrow daggerThrow : CoreAbility.getAbilities(DaggerThrow.class)) {
			if(daggerThrow.getDaggers().stream().anyMatch(dagger -> dagger.getArrow().getUniqueId() == arrow.getUniqueId())) {
				return daggerThrow;
			}
		}
		return null;
	}
	
	public boolean doesRequireArrow() {
		return this.requiredArrows > 0;
	}
	
	public boolean isParticlesEnabled() {
		return this.particles;
	}
	
	public boolean isCosmeticsEnabled() {
		return this.cosmetics;
	}
	
	public List<Dagger> getDaggers() {
		return this.daggers;
	}
	
	public Map<Integer, Integer> getHitCount() {
		return this.hitCount;
	}
	
	public int getMaximumHits() {
		return this.maxHits;
	}
	
	//Based on the code in JedCore
	public static boolean removeItem(final Player player, final Material material, final int amount) {
		for(final ItemStack item : player.getInventory().getContents()) {
			if(item == null || item.getType() != material || item.getAmount() < amount)
				continue;
			
			else if(item.getAmount() > amount)
				item.setAmount(item.getAmount() - amount);
			
			else if(item.getAmount() == amount) {
				if(!player.getInventory().removeItem(item).isEmpty()) {
					final ItemStack offhand = player.getInventory().getItemInOffHand();

					// Spigot seems to not handle offhand correctly with removeItem, so try to manually remove it.
					if (offhand != null && offhand.getType() == material && offhand.getAmount() == amount) {
						player.getInventory().setItemInOffHand(null);
					}
				}
			}
			
			return true;
		}
		
		return false;
	}
}
