package com.dreamerboy.daggers;

import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ElementalAbility;

public abstract class DaggersAbility extends ElementalAbility implements AddonAbility {

	public DaggersAbility(Player player) {
		super(player);
	}

	@Override
	public Element getElement() {
		return CustomElement.DAGGERS;
	}

	@Override
	public boolean isExplosiveAbility() {
		return false;
	}

	@Override
	public boolean isIgniteAbility() {
		return false;
	}

	@Override
	public String getAuthor() {
		return "DreamerBoy / Dramaura";
	}

	@Override
	public String getVersion() {
		return "v2.0.0";
	}

	@Override
	public void load() {
		
	}

	@Override
	public void stop() {
		super.remove();
	}

}
