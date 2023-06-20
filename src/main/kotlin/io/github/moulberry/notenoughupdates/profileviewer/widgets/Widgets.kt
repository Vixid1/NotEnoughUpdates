/*
 * Copyright (C) 2023 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.moulberry.notenoughupdates.profileviewer.widgets

import io.github.moulberry.notenoughupdates.profileviewer.widgets.collection.CollectionCountWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.collection.CollectionSelectorWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.collection.MinionCountWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.crimsonisle.DojoStatsWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.crimsonisle.FactionDataWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.crimsonisle.KuudraRunsWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.crimsonisle.MatriarchAttemptWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.dungeons.*
import io.github.moulberry.notenoughupdates.profileviewer.widgets.fishing.ThunderJawbusKillsWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.fishing.TotalTrophyFishWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.fishing.TrophyFishDisplayWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.fishing.TrophyFishProgressWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.inventory.EquippedGearWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.inventory.InventoryDisplayWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.inventory.InventorySelectorWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.level.*
import io.github.moulberry.notenoughupdates.profileviewer.widgets.mining.CrystalsCollectedWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.mining.HotmLevelWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.mining.HotmTreeWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.mining.PowderCountWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.misc.*
import io.github.moulberry.notenoughupdates.profileviewer.widgets.pets.PetInfoWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.pets.PetPanoramaWidget
import io.github.moulberry.notenoughupdates.profileviewer.widgets.pets.PetSelectorWidget
import kotlin.reflect.KClass

enum class WidgetFlags {
    NONE,
    NORMAL,
    MASTER
}

enum class Widgets(
    val widgetId: Int,
    val widgetName: String,
    val classDef: KClass<out WidgetInterface>,
    val flags: WidgetFlags?
) {
    PLAYER_MODEL(0, "Player Model", PlayerModelWidget::class, WidgetFlags.NONE),
    WEIGHT_NETWORTH(1,  "Weight and Networth", WeightNetworthWidget::class, WidgetFlags.NONE),
    SKILLS_SLAYERS(2, "Skills and Slayers", SkillsSlayersWidget::class, WidgetFlags.NONE),
    LEVEL_WIDGET(3, "Level Overview", LevelWidget::class, WidgetFlags.NONE),
    LEVEL_BREAKDOWN_A(4, "Level Breakdown 1", LevelBreakdownAWidget::class, WidgetFlags.NONE),
    LEVEL_BREAKDOWN_B(5, "Level Breakdown 2", LevelBreakdownBWidget::class, WidgetFlags.NONE),
    LEVEL_SMALL(6, "Level Single Line", LevelSmallWidget::class, WidgetFlags.NONE),
    CATACOMBS_LEVEL(7, "Catacombs Level and Calculation", CatacombsLevelWidget::class, WidgetFlags.NONE),
    CLASS_LEVELS(8, "Class Levels", ClassLevelsWidget::class, WidgetFlags.NONE),
    BOSS_COLLECTION_NORMAL(9, "Boss Collection (Normal)", BossCollectionWidget::class, WidgetFlags.NORMAL),
    BOSS_COLLECTION_MASTER(10, "Boss Collection (Master)", BossCollectionWidget::class, WidgetFlags.MASTER),
    RUNS_SECRETS_NORMAL(11, "Runs and Secrets (Normal)", RunsSecretsWidget::class, WidgetFlags.NORMAL),
    RUNS_SECRETS_MASTER(12, "Runs and Secrets (Master)", RunsSecretsWidget::class, WidgetFlags.MASTER),
    FASTEST_RUNS_NORMAL(13, "Fastest Runs (Normal)", FastestRunsWidget::class, WidgetFlags.NORMAL),
    FASTEST_RUNS_MASTER(14, "Fastest Runs (Master)", FastestRunsWidget::class, WidgetFlags.MASTER),
    PURSE_BANK_WIDGET(15, "Purse, Bank, Join Date and Guild", PurseBankWidget::class, WidgetFlags.NONE),
    FAIRY_AVERAGE_LEVEL(16, "Fairy Souls and Average Levels", FairyAverageLevelWidget::class, WidgetFlags.NONE),
    AUCTION_STATS(17, "Auction Statistics", AuctionStatsWidget::class, WidgetFlags.NONE),
    ESSENCE(18, "Essence", EssenceWidget::class, WidgetFlags.NONE),
    MINING_FISHING_STATS(19, "Mining and Fishing Stats", MiningFishingStatsWidget::class, WidgetFlags.NONE),
    KILLS(20, "Kills", KillsWidgets::class, WidgetFlags.NONE),
    DEATHS(21, "Deaths", DeathsWidget::class, WidgetFlags.NONE),
    KILL_DEATH_SEARCH_BAR(22, "Kills and Deaths Search Bar", KillDeathSearchWidget::class, WidgetFlags.NONE),
    EQUIPPED_GEAR(23, "Equipped Gear", EquippedGearWidget::class, WidgetFlags.NONE),
    INVENTORY_DISPLAY(24, "Inventory Display", InventoryDisplayWidget::class, WidgetFlags.NONE),
    INVENTORY_SELECTOR(25, "Inventory Selector + Search Bar", InventorySelectorWidget::class, WidgetFlags.NONE),
    COLLECTION_SELECTOR(26, "Collection Selector", CollectionSelectorWidget::class, WidgetFlags.NONE),
    COLLECTION_COUNT(27,"Collection Count", CollectionCountWidget::class, WidgetFlags.NONE),
    MINION_COUNT(28, "Minion Count", MinionCountWidget::class, WidgetFlags.NONE),
    PET_PANORAMA(29,"Pet Panorama", PetPanoramaWidget::class, WidgetFlags.NONE),
    PET_INFO(30, "Pet Information", PetInfoWidget::class, WidgetFlags.NONE),
    PET_SELECTOR(31, "Pet Selector", PetSelectorWidget::class, WidgetFlags.NONE),
    HOTM_LEVEL(32, "HOTM Level", HotmLevelWidget::class, WidgetFlags.NONE),
    POWDER_COUNT(33, "Powder Count", PowderCountWidget::class, WidgetFlags.NONE),
    CRYSTALS_COLLECTED(34, "Crystals Collected", CrystalsCollectedWidget::class, WidgetFlags.NONE),
    HOTM_TREE(35, "HOTM Tree", HotmTreeWidget::class, WidgetFlags.NONE),
    TOTAL_TROPHY_FISH(36, "Total Trophy Fish Caught", TotalTrophyFishWidget::class, WidgetFlags.NONE),
    TROPHY_FISH_DISPLAY(37, "Trophy Fish Display", TrophyFishDisplayWidget::class, WidgetFlags.NONE),
    THUNDER_JAWBUS_KILLS(38, "Thunder + Jawbus Kills", ThunderJawbusKillsWidget::class, WidgetFlags.NONE),
    TROPHY_FISH_PROGRESS(39, "Trophy Fish Progress", TrophyFishProgressWidget::class, WidgetFlags.NONE),
    BESTIARY(40, "Bestiary", BestiaryWidget::class, WidgetFlags.NONE),
    KUUDRA_RUNS(41, "Kuudra Runs", KuudraRunsWidget::class, WidgetFlags.NONE),
    DOJO_STATS(42, "Dojo Stats", DojoStatsWidget::class, WidgetFlags.NONE),
    FACTION_DATA(43, "Faction Data", FactionDataWidget::class, WidgetFlags.NONE),
    MATRIARCH_ATTEMPT(44, "Matriarch Attempt", MatriarchAttemptWidget::class, WidgetFlags.NONE);

    companion object {
        private val idMap = hashMapOf<Int, Widgets>()

        init {
            for (each in Widgets.values()) {
                idMap[each.widgetId] = each
            }
        }

        fun getWidget(id: Int?) : Widgets? {
            return Widgets.idMap[id]
        }
    }
}
