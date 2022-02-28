package com.dreamerboy.daggers.abilities;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.dreamerboy.daggers.CustomElement;
import com.dreamerboy.daggers.Daggers;
import com.dreamerboy.daggers.DaggersAbility;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.MovementHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;

public class Dagger extends DaggersAbility {
	
	public enum DaggerType {
		NO_EFFECT("normal", "Normal Daggers"), //255, 255, 102
		POISONOUS("poisonous", "Poisonous Daggers"), //41, 163, 41
		EXPLOSIVE("explosive", "Explosive Daggers"), //204, 0, 0
		FORCEFIELD("forcefield", "ForceField Daggers"), //153, 153, 255
		ELECTRO("electro", "Electro Daggers"); //0, 115, 230
		
		private final String name, display;
		
		private DaggerType(final String name, final String display) {
			this.name = name;
			this.display = display;
		}
		
		public String getDisplay() {
			return display;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		public boolean isEnabled() {
			if(this == POISONOUS)
				return DaggerThrow.poisonous;
			else if(this == EXPLOSIVE)
				return DaggerThrow.explosive;
			else if(this == FORCEFIELD)
				return DaggerThrow.forcefield;
			else if(this == ELECTRO)
				return DaggerThrow.electro;
			return true;
		}
		
		public Color getColor() {
			if(this == POISONOUS)
				return Color.fromRGB(41, 163, 41);
			else if(this == EXPLOSIVE)
				return Color.fromRGB(204, 0, 0);
			else if(this == FORCEFIELD)
				return Color.fromRGB(153, 153, 255);
			else if(this == ELECTRO)
				return Color.fromRGB(0, 115, 230);
			return Color.fromRGB(255, 255, 102);
		}
	}
	
	public static final Material[] UNBREAKABLE_MATERIALS = {
			Material.AIR, Material.CAVE_AIR, Material.BARRIER, Material.BEDROCK, Material.OBSIDIAN, 
			Material.CRYING_OBSIDIAN, Material.NETHER_PORTAL, Material.END_PORTAL, Material.END_PORTAL_FRAME, 
			Material.CHEST, Material.CHEST_MINECART, Material.ENDER_CHEST, Material.TRAPPED_CHEST, Material.BARREL,
			Material.SHULKER_BOX, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.DISPENSER,
			Material.DROPPER, Material.ENCHANTING_TABLE, Material.BREWING_STAND, Material.BEACON, Material.ANVIL,
			Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.GRINDSTONE, Material.CARTOGRAPHY_TABLE, Material.LOOM,
			Material.SMITHING_TABLE, Material.ARMOR_STAND
	};
	
	private final DaggerThrow parentAbility;
	private final Arrow arrow;
	private final DaggerType type;
	private final Location origin;
	
	private double damage;
	
	//forcefield
	private double fieldRadius, fieldKnockback, fieldDuration;
	
	//electro
	private double electroRadius, electroDuration;
	private int chainAmount;
	
	//poisonous
	private double poisonRadius;
	private int poisonDuration, poisonAmplifier;
			
	//explosive
	private double explosionDamage;
	private int explosionPower;
	private long revertTime;
	private boolean breakBlocks, setFire, revertExplosion;
	
	public Dagger(final DaggerThrow parentAbility, final DaggerType type) {
		super(parentAbility.getPlayer());
		
		setFields();
		
		this.parentAbility = parentAbility;
		this.type = type;
		this.origin = parentAbility.getPlayer().getEyeLocation().clone();
		
		this.arrow = parentAbility.getPlayer().launchProjectile(Arrow.class);
		this.arrow.setKnockbackStrength(0);
		this.arrow.setBounce(false);
		if(!type.equals(DaggerType.NO_EFFECT)) this.arrow.setColor(type.getColor());
		this.arrow.setMetadata("daggerthrow-dagger", new FixedMetadataValue(Daggers.plugin, this));
		this.arrow.setPickupStatus(parentAbility.doesRequireArrow() ? PickupStatus.ALLOWED : PickupStatus.DISALLOWED);
		if(parentAbility.isParticlesEnabled() && (!parentAbility.isCosmeticsEnabled() || (this.type.equals(DaggerType.NO_EFFECT) && !DaggerThrow.COSMETICS.containsKey(this.player.getUniqueId()))))
			this.arrow.setCritical(true);
		
		if(this.type.equals(DaggerType.FORCEFIELD))
			this.arrow.setGravity(false);
		
		start();
	}
	
	private void setFields() {
		final String path = "ExtraAbilities.DreamerBoy.Chi.DaggerThrow.";
		
		this.damage = ConfigManager.getConfig().getDouble(path + "Damage");
		
		this.fieldRadius = ConfigManager.getConfig().getDouble(path + "ForceField.Radius");
		this.fieldKnockback = ConfigManager.getConfig().getDouble(path + "ForceField.Knockback");
		this.fieldDuration = ConfigManager.getConfig().getDouble(path + "ForceField.Duration");
		
		this.electroDuration = ConfigManager.getConfig().getDouble(path + "Electro.StunDuration");
		this.electroRadius = ConfigManager.getConfig().getDouble(path + "Electro.Chain.Radius");
		this.chainAmount = ConfigManager.getConfig().getInt(path + "Electro.Chain.Amount");
		
		this.poisonRadius = ConfigManager.getConfig().getDouble(path + "Poisonous.Burst.Radius");
		this.poisonDuration = ConfigManager.getConfig().getInt(path + "Poisonous.Poison.Duration") * 20;
		this.poisonAmplifier = ConfigManager.getConfig().getInt(path + "Poisonous.Poison.Amplifier") - 1;
	
		this.explosionDamage = ConfigManager.getConfig().getDouble(path + "Explosive.Explosion.Damage");
		this.explosionPower = ConfigManager.getConfig().getInt(path + "Explosive.Explosion.Power");
		this.breakBlocks = ConfigManager.getConfig().getBoolean(path + "Explosive.Explosion.BreakBlocks.Enabled");
		this.setFire = ConfigManager.getConfig().getBoolean(path + "Explosive.Explosion.BreakBlocks.PlaceFire");
		this.revertExplosion = ConfigManager.getConfig().getBoolean(path + "Explosive.Explosion.BreakBlocks.RevertExplosion.Enabled");
		this.revertTime = ConfigManager.getConfig().getLong(path + "Explosive.Explosion.BreakBlocks.RevertExplosion.RevertTime");
	}
	
	@Override
	public boolean isHiddenAbility() {
		return true;
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public Location getLocation() {
		return this.arrow.getLocation();
	}

	@Override
	public String getName() {
		return "Dagger";
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
	public void remove() {
		super.remove();
		
		if(!this.arrow.isInBlock()) 
			this.arrow.setVelocity(new Vector());
		
		if(!this.type.equals(DaggerType.NO_EFFECT) || !this.parentAbility.doesRequireArrow())
			this.arrow.remove();
	}

	@Override
	public void progress() {
		if(DaggerThrow.fusion && !this.bPlayer.getBoundAbilityName().equals(this.parentAbility.getName())) {
			remove();
			return;
		}
		
		if(!this.arrow.isInBlock()) {
			if(this.type.equals(DaggerType.FORCEFIELD)) {
				if(System.currentTimeMillis() > getStartTime() + fieldDuration) {
					remove();
					return;
				}
				
				if(this.parentAbility.isParticlesEnabled() && this.parentAbility.isCosmeticsEnabled()) {
					if(Math.random() < .45) {
						final DustOptions dustOptions = new DustOptions(DaggerThrow.COSMETICS.getOrDefault(parentAbility.getPlayer().getUniqueId(), Color.fromRGB(153, 153, 255)), 1.5f);
						
						for (int angle = 0; angle <= 360; angle += 15) {
							ParticleEffect.REDSTONE.display(this.getLocation().clone().add(GeneralMethods.getOrthogonalVector(this.origin.getDirection().clone(), angle, 1.25)), 1, 0.0F, 0.0F, 0.0F, 0.06F, dustOptions);
						}
					}
				}
				
				final List<Integer> daggerIDS = this.parentAbility.getDaggers().stream().map(Dagger::getArrow).map(Arrow::getEntityId).collect(Collectors.toList());
				
				for(final Entity entity : GeneralMethods.getEntitiesAroundPoint(this.getLocation(), this.fieldRadius)) {
					if(!(entity instanceof ArmorStand) && entity.getUniqueId() != this.player.getUniqueId() && !(daggerIDS.contains(entity.getEntityId()))) {
						final Vector vector = entity.getLocation().toVector().subtract(this.arrow.getLocation().toVector());
						entity.setVelocity(vector.normalize().multiply(this.fieldKnockback));
					}
				}
			} else {
				if(this.parentAbility.isParticlesEnabled() && this.parentAbility.isCosmeticsEnabled()) {
					Color color = this.type.getColor();
					
					if(DaggerThrow.COSMETICS.containsKey(player.getUniqueId()))
						color = DaggerThrow.COSMETICS.get(player.getUniqueId());
					
					ParticleEffect.REDSTONE.display(this.getLocation(), 1, 0.0D, 0.0D, 0.0D, 0.06D, new DustOptions(color, 0.75f));
				}
			}
		}
	}
	
	public Arrow getArrow() {
		return arrow;
	}
	
	public DaggerThrow getParentAbility() {
		return parentAbility;
	}
	
	public DaggerType getType() {
		return type;
	}

	public void damageEntity(final Entity entity) {
		if(entity instanceof LivingEntity && !(entity instanceof ArmorStand) && entity.getUniqueId() != this.arrow.getUniqueId() && entity.getUniqueId() != player.getUniqueId() && this.parentAbility.getHitCount().getOrDefault(entity.getEntityId(), 0)+1 <= this.parentAbility.getMaximumHits()) {
			DamageHandler.damageEntity(entity, this.damage, this.parentAbility);
			((LivingEntity) entity).setNoDamageTicks(0);
			if(this.type.equals(DaggerType.POISONOUS))
				((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, this.poisonDuration, this.poisonAmplifier, true));
			else if(this.type.equals(DaggerType.EXPLOSIVE))
				this.createExplosion(entity.getLocation());
			else if(this.type.equals(DaggerType.ELECTRO)) {
				electrocute(entity, this.electroDuration);
			}
			this.parentAbility.getHitCount().put(entity.getEntityId(), this.parentAbility.getHitCount().getOrDefault(entity.getEntityId(), 0)+1);
		}
	}
	
	private void drawLine(final Location origin, final Location target) {
		final Location location = origin.clone();
		final DustOptions dustOptions = new DustOptions(DaggerType.ELECTRO.getColor(), 1.25F);
		
		for(double d = 0; d < (origin.distance(target)/.2); d++) {
			final Vector direction = target.clone().toVector().subtract(location.clone().toVector());
			location.add(direction.normalize().multiply(.2));
			ParticleEffect.REDSTONE.display(location, 1, 0.0D, 0.0D, 0.0D, 0.06D, dustOptions);
		}
	}
	
	private void electrocute(final Entity entity, final double duration) {
		final DustOptions dustOptions = new DustOptions(DaggerType.ELECTRO.getColor(), 1.25F);
		
		new MovementHandler((LivingEntity) entity, this.parentAbility)
		.stopWithDuration((long) duration, CustomElement.DAGGERS.getColor() + "* Electrocuted *");
		
		new BukkitRunnable() {
			int i = 0;
			
			@Override
			public void run() {
				if(i > duration || entity == null || entity.isDead() || (entity instanceof Player && !((Player) entity).isOnline())) {
					entity.removeMetadata("electrodaggers", Daggers.plugin);
					this.cancel();
					return;
				}
				
				ParticleEffect.REDSTONE.display(entity.getLocation().add(0,1,0), 1, 1.0D, 1.0D, 1.0D, 0.06D, dustOptions);
				i++;
			}
		}.runTaskTimer(Daggers.plugin, 0, 1);
		
		entity.setMetadata("electrodaggers", new FixedMetadataValue(Daggers.plugin, this));
		
		if(chainAmount-- >= 0) {
			for(final Entity e : GeneralMethods.getEntitiesAroundPoint(entity.getLocation(), this.electroRadius)) {
				if(e instanceof LivingEntity && !(e instanceof ArmorStand) && e.getUniqueId() != player.getUniqueId() && !e.hasMetadata("electrodaggers")) {
					drawLine(entity.getLocation().add(0,1,0), e.getLocation().add(0,1,0));
					electrocute(e, this.electroDuration/2);
				}
			}
		}
	}
	
	public void createExplosion(final Location location) {
		if(this.breakBlocks) {
			for(final Block b : GeneralMethods.getBlocksAroundPoint(location, this.explosionPower)) {
				if(GeneralMethods.isRegionProtectedFromBuild(this.parentAbility, b.getLocation()) || isUnbreakable(b)) {
					continue;
				}
				
				if(this.revertExplosion)
					new TempBlock(b, Material.AIR.createBlockData(), this.revertTime);
				else b.setType(Material.AIR);
				
				if (this.setFire && b.getRelative(BlockFace.DOWN).getType().isSolid() && Math.random() < .30)
					b.setType(Material.FIRE);
			}
		}
		ParticleEffect.EXPLOSION_HUGE.display(location, 2, 1f, 1f, 1f, 0.06f);
		location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
		for(final Entity entity : GeneralMethods.getEntitiesAroundPoint(location, this.explosionPower)) {
			if(entity instanceof LivingEntity && !(entity instanceof ArmorStand) && entity.getUniqueId() != this.parentAbility.getPlayer().getUniqueId()) {
				DamageHandler.damageEntity(entity, this.explosionDamage, this.parentAbility);
			}
		}
	}
	
	public void createPoisonBurst(final Location loc) {
		new BukkitRunnable() {
			int radius = 1;
			final Location location = loc.clone();
			final DustOptions dustOptions = new DustOptions(DaggerThrow.COSMETICS.getOrDefault(parentAbility.getPlayer().getUniqueId(), DaggerType.POISONOUS.getColor()), 1);
			@Override
			public void run() {
				if(parentAbility.getPlayer() == null || !parentAbility.getPlayer().isOnline() || parentAbility.getPlayer().isDead()) {
					cancel();
					return;
				}
				if(radius < poisonRadius) {
					for(final Entity entity : GeneralMethods.getEntitiesAroundPoint(loc, radius)) {
						if(entity instanceof LivingEntity && entity.getUniqueId() != parentAbility.getPlayer().getUniqueId()) {
				    		((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration/2, poisonAmplifier, true));
				    	}
					}
					for (double i = 0; i <= Math.PI; i += Math.PI / 10) {
						double radius = Math.sin(i) * this.radius;
						double y = Math.cos(i) * this.radius; 
						for (double a = 0; a < Math.PI * 2; a+= Math.PI / 10) {
						      double x = Math.cos(a) * radius;
						      double z = Math.sin(a) * radius;
						      location.add(x, y, z);
						      ParticleEffect.REDSTONE.display(location, 1, 0.0F, 0.0F, 0.0F, 0.06F, dustOptions);
						      location.subtract(x, y, z);
						}
					}
					radius++;
				} else {
					cancel();
				}
			}
		}.runTaskTimer(Daggers.plugin, 0, 1);
	}
	
	public static boolean isUnbreakable(final Block block) {
		return Arrays.asList(UNBREAKABLE_MATERIALS).contains(block.getType()) || block.getState() instanceof InventoryHolder 
				|| block.getState() instanceof CreatureSpawner || block.getType().toString().contains("_SIGN");
	}
	
	public static Dagger getAbilityFromArrow(final Arrow arrow) {
		for(final Dagger dagger : CoreAbility.getAbilities(Dagger.class)) {
			if(dagger.getArrow().getUniqueId() == arrow.getUniqueId()) {
				return dagger;
			}
		}
		return null;
	}
}
