package com.dreamerboy.daggers;

import org.bukkit.ChatColor;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.Element.ElementType;
import com.projectkorra.projectkorra.Element.SubElement;

public class CustomElement {

	public static final SubElement DAGGERS = new SubElement("Daggers", Element.CHI, ElementType.NO_SUFFIX, Daggers.plugin) {
		public ChatColor getColor() {
			return ChatColor.YELLOW;
		}
	};
	
}
