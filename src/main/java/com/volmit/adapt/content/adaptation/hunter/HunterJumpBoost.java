/*------------------------------------------------------------------------------
 -   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
 -   Copyright (c) 2022 Arcane Arts (Volmit Software)
 -
 -   This program is free software: you can redistribute it and/or modify
 -   it under the terms of the GNU General Public License as published by
 -   the Free Software Foundation, either version 3 of the License, or
 -   (at your option) any later version.
 -
 -   This program is distributed in the hope that it will be useful,
 -   but WITHOUT ANY WARRANTY; without even the implied warranty of
 -   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 -   GNU General Public License for more details.
 -
 -   You should have received a copy of the GNU General Public License
 -   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 -----------------------------------------------------------------------------*/

package com.volmit.adapt.content.adaptation.hunter;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

public class HunterJumpBoost extends SimpleAdaptation<HunterJumpBoost.Config> {
    public HunterJumpBoost() {
        super("hunter-jumpboost");
        registerConfiguration(Config.class);
        setDescription(Adapt.dLocalize("Hunter", "HunterJumpBoost", "Description"));
        setDisplayName(Adapt.dLocalize("Hunter", "HunterJumpBoost", "Name"));
        setIcon(Material.PUFFERFISH_BUCKET);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GRAY + Adapt.dLocalize("Hunter", "HunterJumpBoost", "Lore1"));
        v.addLore(C.GREEN + "+ " + level + C.GRAY + Adapt.dLocalize("Hunter", "HunterJumpBoost", "Lore2"));
        v.addLore(C.RED + "- " + 5 + level + C.GRAY + Adapt.dLocalize("Hunter", "HunterJumpBoost", "Lore3"));
        v.addLore(C.GRAY + "* " + level + C.GRAY + " " +Adapt.dLocalize("Hunter", "HunterJumpBoost", "Lore4"));
        v.addLore(C.GRAY + "* " + level + C.GRAY + " " +Adapt.dLocalize("Hunter", "HunterJumpBoost", "Lore5"));
    }


    @EventHandler
    public void on(EntityDamageEvent e) {
        if (e.getEntity() instanceof org.bukkit.entity.Player p && !e.getCause().equals(EntityDamageEvent.DamageCause.STARVATION) && hasAdaptation(p)) {
            addPotionStacks(p, PotionEffectType.HUNGER, 5 + getLevel(p), 100, true);
            addPotionStacks(p, PotionEffectType.SPEED, getLevel(p), 50, false);
        }
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
        int baseCost = 4;
        int maxLevel = 5;
        int initialCost = 8;
        double costFactor = 0.4;
    }
}
