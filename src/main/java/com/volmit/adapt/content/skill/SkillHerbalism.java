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

package com.volmit.adapt.content.skill;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.api.world.AdaptStatTracker;
import com.volmit.adapt.content.adaptation.herbalism.*;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.advancements.advancement.AdvancementDisplay;
import com.volmit.adapt.util.advancements.advancement.AdvancementVisibility;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.meta.PotionMeta;

public class SkillHerbalism extends SimpleSkill<SkillHerbalism.Config> {
    public SkillHerbalism() {
        super("herbalism", Adapt.dLocalize("Skill", "Herbalism", "Icon"));
        registerConfiguration(Config.class);
        setColor(C.GREEN);
        setInterval(3700);
        setDescription(Adapt.dLocalize("Skill", "Herbalism", "Description"));
        setDisplayName(Adapt.dLocalize("Skill", "Herbalism", "Name"));
        setIcon(Material.WHEAT);
        registerAdaptation(new HerbalismGrowthAura());
        registerAdaptation(new HerbalismReplant());
        registerAdaptation(new HerbalismHungryShield());
        registerAdaptation(new HerbalismHungryHippo());
        registerAdaptation(new HerbalismDropToInventory());
        registerAdaptation(new HerbalismLuck());
        registerAdvancement(AdaptAdvancement.builder()
                .icon(Material.COOKED_BEEF)
                .key("challenge_eat_100")
                .title("So much to eat!")
                .description("Eat over 100 Items!")
                .frame(AdvancementDisplay.AdvancementFrame.CHALLENGE)
                .visibility(AdvancementVisibility.PARENT_GRANTED)
                .child(AdaptAdvancement.builder()
                        .icon(Material.COOKED_BEEF)
                        .key("challenge_eat_1000")
                        .title("Unquenchable Hunger!")
                        .description("Eat over 1,000 Items!")
                        .frame(AdvancementDisplay.AdvancementFrame.CHALLENGE)
                        .visibility(AdvancementVisibility.PARENT_GRANTED)
                        .build())
                .build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_eat_100").goal(100).stat("food.eaten").reward(getConfig().challengeEat100Reward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_eat_1000").goal(1000).stat("food.eaten").reward(getConfig().challengeEat1kReward).build());
        registerAdvancement(AdaptAdvancement.builder()
                .icon(Material.COOKED_BEEF)
                .key("challenge_harvest_100")
                .title("Full Harvest")
                .description("Harvest over 100 crops!")
                .frame(AdvancementDisplay.AdvancementFrame.CHALLENGE)
                .visibility(AdvancementVisibility.PARENT_GRANTED)
                .child(AdaptAdvancement.builder()
                        .icon(Material.COOKED_BEEF)
                        .key("challenge_harvest_1000")
                        .title("Grand Harvest")
                        .description("Harvest 1,000 crops!")
                        .frame(AdvancementDisplay.AdvancementFrame.CHALLENGE)
                        .visibility(AdvancementVisibility.PARENT_GRANTED)
                        .build())
                .build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_harvest_100").goal(100).stat("harvest.blocks").reward(getConfig().challengeHarvest100Reward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_harvest_1000").goal(1000).stat("harvest.blocks").reward(getConfig().challengeHarvest1kReward).build());
    }

    @EventHandler
    public void on(PlayerItemConsumeEvent e) {
        if (e.getItem().getItemMeta() instanceof PotionMeta o) {
            return;
        }

        xp(e.getPlayer(), getConfig().foodConsumeXP);
        getPlayer(e.getPlayer()).getData().addStat("food.eaten", 1);
    }

    @EventHandler
    public void on(PlayerShearEntityEvent e) {
        xp(e.getPlayer(), e.getEntity().getLocation(), getConfig().shearXP);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerHarvestBlockEvent e) {
        if (e.isCancelled()) {
            return;
        }

        if (e.getHarvestedBlock().getBlockData() instanceof Ageable) {
            getPlayer(e.getPlayer()).getData().addStat("harvest.blocks", 1);
            xp(e.getPlayer(), e.getHarvestedBlock().getLocation().clone().add(0.5, 0.5, 0.5), getConfig().harvestPerAgeXP * (((Ageable) e.getHarvestedBlock().getBlockData()).getAge()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BlockPlaceEvent e) {
        if (e.getBlock().getBlockData() instanceof Ageable) {
            xp(e.getPlayer(), e.getBlock().getLocation().clone().add(0.5, 0.5, 0.5), getConfig().plantCropSeedsXP);
            getPlayer(e.getPlayer()).getData().addStat("harvest.planted", 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerInteractEvent e) {
        if (e.useItemInHand().equals(Event.Result.DENY)) {
            return;
        }

        if (e.getClickedBlock() == null) {
            return;
        }

        if (e.getClickedBlock().getType().equals(Material.COMPOSTER)) {
            Levelled c = ((Levelled) e.getClickedBlock().getBlockData());
            int ol = c.getLevel();

            J.s(() -> {
                int nl = ((Levelled) e.getClickedBlock().getBlockData()).getLevel();
                if (nl > ol || (ol > 0 && nl == 0)) {
                    xp(e.getPlayer(), e.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5), getConfig().composterBaseXP + (nl * getConfig().composterLevelXPMultiplier) + (nl == 0 ? getConfig().composterNonZeroLevelBonus : 5));
                    getPlayer(e.getPlayer()).getData().addStat("harvest.composted", 1);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BlockBreakEvent e) {
        if (e.getBlock().getType().equals(Material.CACTUS)) {
            return;
        }

        if (e.getBlock().getBlockData() instanceof Ageable) {
            xp(e.getPlayer(), e.getBlock().getLocation().clone().add(0.5, 0.5, 0.5), getConfig().harvestPerAgeXP * (((Ageable) e.getBlock().getBlockData()).getAge()));
            getPlayer(e.getPlayer()).getData().addStat("harvest.blocks", 1);
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
    public static class Config {
        public boolean enabled = true;
        public double foodConsumeXP = 125;
        public double shearXP = 95;
        public double harvestPerAgeXP = 35;
        public double plantCropSeedsXP = 4;
        public double composterBaseXP = 51;
        public double composterLevelXPMultiplier = 3;
        public double composterNonZeroLevelBonus = 250;
        public double challengeEat100Reward = 1250;
        public double challengeEat1kReward = 6250;
        public double challengeHarvest100Reward = 1250;
        public double challengeHarvest1kReward = 6250;
    }
}
