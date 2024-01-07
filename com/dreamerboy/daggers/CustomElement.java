package com.dreamerboy.daggers;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;
import com.projectkorra.projectkorra.configuration.ConfigManager;

import net.md_5.bungee.api.ChatColor;

public class CustomElement {

	public static final SubElement DAGGERS = new SubElement("Daggers", Element.CHI, ElementType.NO_SUFFIX, Daggers.plugin) {
	
		@Override
		public net.md_5.bungee.api.ChatColor getColor() {
			final String color = ConfigManager.languageConfig.get().getString("Chat.Colors.Daggers");
			if(color == null)
				return ChatColor.YELLOW;
			return ChatColor.of(color);
		};
		
	};
	
}
