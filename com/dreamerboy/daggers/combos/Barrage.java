package com.dreamerboy.daggers.combos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.dreamerboy.daggers.Daggers;
import com.dreamerboy.daggers.DaggersAbility;
import com.dreamerboy.daggers.abilities.DaggerThrow;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager.AbilityInformation;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class Barrage extends DaggersAbility implements ComboAbility, AddonAbility {

	private long cooldown;
	private int amount, interval, tick;
	private boolean started, requireArrow;
	private double damage;
	private List<BarrageDagger> daggers = new ArrayList<>();
	
	public Barrage(Player player) {
		super(player);
		
		if(hasAbility(player, DaggerThrow.class) || !bPlayer.getAbilities().containsValue("DaggerThrow") || !bPlayer.canBendIgnoreBinds(this) || hasAbility(player, this.getClass()))
			return;
		
		setFields();
		start();
	}
	
	private void setFields() {
		final String path = "ExtraAbilities.DreamerBoy.Chi.Barrage.";
		
		this.cooldown = ConfigManager.getConfig().getLong(path + "Cooldown");
		this.interval = ConfigManager.getConfig().getInt(path + "Interval");
		this.amount = ConfigManager.getConfig().getInt(path + "Amount");
		this.damage = ConfigManager.getConfig().getDouble(path + "Damage");
		this.requireArrow = ConfigManager.getConfig().getBoolean(path + "RequireArrow");
	}

	@Override
	public boolean isEnabled() {
		return getConfig().getBoolean("ExtraAbilities.DreamerBoy.Chi.Barrage.Enabled");
	}
	
	@Override
	public long getCooldown() {
		return this.cooldown;
	}

	@Override
	public List<Location> getLocations() {
		return super.getLocations();
	}
	
	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public String getName() {
		return "Barrage";
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public String getDescription() {
		return "Barrage allows you to hurl sharp daggers at an incredible pace, creating a continuous stream of projectiles that swiftly overwhelm your target.";
	}
	
	@Override
	public String getInstructions() {
		return "RapidPunch (Tap Sneak) > Change your slot to DaggerThrow";
	}
	
	@Override
	public void remove() {
		super.remove();
		if(this.started)
			bPlayer.addCooldown(this);
	}
	
	@Override
	public void progress() {
		if(!started) {
			if(!bPlayer.canBendIgnoreBinds(this) || System.currentTimeMillis() > this.getStartTime() + 2000) {
				remove();
				return;
			}
			
			if(bPlayer.getBoundAbilityName().equalsIgnoreCase("DaggerThrow")) {
				this.started = true;
				return;
			} else if(!bPlayer.getBoundAbilityName().equalsIgnoreCase("RapidPunch")) {
				remove();
				return;
			}
		} else {
			if(this.amount > 0) {
				if(!bPlayer.canBendIgnoreBinds(this) || !bPlayer.getBoundAbilityName().equalsIgnoreCase("DaggerThrow")) {
					remove();
					return;
				}
				
				if(tick % interval == 0)
					shootDagger();
				tick++;
			}
			
			this.daggers.removeIf(dagger -> !dagger.run());
			
			if(this.daggers.isEmpty() && this.amount <= 0) {
				remove();
				return;
			}
		}
	}
	
	public void damageEntity(final Entity entity) {
		((LivingEntity) entity).setNoDamageTicks(0);
		DamageHandler.damageEntity(entity, this.damage, this);
	}
	
	private void shootDagger() {
		if(this.requireArrow && DaggerThrow.removeItem(player, Material.ARROW, 1) == null) {
			remove();
			return;
		}
		
		final Entity targetedEntity = GeneralMethods.getTargetedEntity(this.player, 20);
		Location targetedLocation = GeneralMethods.getTargetedLocation(this.player, 20);
		if(targetedEntity != null)
			targetedLocation = targetedEntity.getLocation().add(0, 1, 0);
		targetedLocation.setPitch(0);
		targetedLocation.setYaw(0);
		
		Vector vector = targetedLocation.toVector().subtract(this.player.getEyeLocation().toVector());
		if(vector.isZero())
			vector = player.getEyeLocation().getDirection();
		
		final BarrageDagger dagger = new BarrageDagger(vector);
		this.daggers.add(dagger);
		player.getLocation().getWorld().playSound(player.getLocation().add(0,2,0), Sound.ITEM_TRIDENT_THROW, 1f, 1.3f);
		amount--;
	}
	
	public static Barrage getAbilityFromArrow(final Arrow arrow) {
		for(final Barrage barrage : CoreAbility.getAbilities(Barrage.class)) {
			if(barrage.daggers.stream().map(dagger -> dagger.arrow).anyMatch(dagger -> dagger.getUniqueId() == arrow.getUniqueId())) {
				return barrage;
			}
		}
		return null;
	}
	
	public BarrageDagger getDaggerFromArrow(final Arrow arrow) {
		for(final BarrageDagger dagger : this.daggers) {
			if(dagger.arrow.getUniqueId() == arrow.getUniqueId())
				return dagger;
		}
		
		return null;
	}

	@Override
	public Object createNewComboInstance(Player player) {
		return new Barrage(player);
	}

	@Override
	public ArrayList<AbilityInformation> getCombination() {
		return new ArrayList<>(Arrays.asList(new AbilityInformation("RapidPunch", ClickType.SHIFT_DOWN), new AbilityInformation("RapidPunch", ClickType.SHIFT_UP)));
	}
	
	public class BarrageDagger {
		
		private Arrow arrow;
		private boolean removed;
		
		public BarrageDagger(final Vector vector) {
			this.arrow = Barrage.this.player.launchProjectile(Arrow.class);
			this.arrow.setKnockbackStrength(0);
			this.arrow.setMetadata("daggerthrow-dagger", new FixedMetadataValue(Daggers.plugin, this));
			this.arrow.setPickupStatus(Barrage.this.requireArrow ? PickupStatus.ALLOWED : PickupStatus.DISALLOWED);
			if(DaggerThrow.particles && (!DaggerThrow.cosmetics || (!DaggerThrow.COSMETICS.containsKey(Barrage.this.player.getUniqueId()))))
				this.arrow.setCritical(true);
			this.arrow.setVelocity(vector.normalize().multiply(5));
		}
		
		public void remove() {
			if(!this.arrow.isInBlock()) 
				this.arrow.setVelocity(new Vector());
			
			if(!Barrage.this.requireArrow)
				this.arrow.remove();
			
			this.removed = true;
		}
		
		public boolean run() {
			if(this.removed || this.arrow.isInBlock() || this.arrow.isDead() || this.arrow.isInWater()) {
				return false;
			}
			
			if(DaggerThrow.particles && DaggerThrow.cosmetics) {
				Color color = Color.fromRGB(255, 255, 102);
				
				if(DaggerThrow.COSMETICS.containsKey(player.getUniqueId()))
					color = DaggerThrow.COSMETICS.get(player.getUniqueId());
				
				ParticleEffect.REDSTONE.display(this.arrow.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.06D, new DustOptions(color, 0.75f));
			}
			
			return true;
		}
	}

}
