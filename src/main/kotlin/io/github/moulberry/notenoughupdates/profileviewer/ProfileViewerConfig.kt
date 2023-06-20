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

package io.github.moulberry.notenoughupdates.profileviewer

import com.google.gson.annotations.Expose

class ProfileViewerConfig(pageList: MutableList<PageConfig>) {

    @Expose
    var pages = pageList

    companion object {
        /**
         * Creates the original PV in the first preset along with 2 empty presets
         */
        @JvmStatic
        fun createDefaultPV() : HashMap<String, ProfileViewerConfig> {
            val presets: HashMap<String, ProfileViewerConfig> = hashMapOf()

            for (i in 0 until 3) {
                if (i == 0) {
                    val presetPages: MutableList<PageConfig> = mutableListOf()

                    val widgets: MutableList<WidgetConfig> = mutableListOf()

                    val widgetConfig = WidgetConfig(
                        0,
                        listOf(200, 100),
                        false
                    )

                    widgets.add(widgetConfig)

                    val sideTabs: MutableList<SidePageConfig> = mutableListOf()

                    val sideTabConfig = SidePageConfig(
                        0,
                        "Side Tab",
                        "b",
                        "PAPER",
                        widgets
                    )

                    sideTabs.add(sideTabConfig)

                    val pageConfig = PageConfig(
                        0,
                        "Main",
                        "a",
                        "BEDROCK",
                        widgets,
                        sideTabs
                    )

                    presetPages.add(pageConfig)

                    presets["preset_$i"] = ProfileViewerConfig(presetPages)
                } else {
                    presets["preset_$i"] = ProfileViewerConfig(mutableListOf())
                }
            }

            return presets
        }
    }
}

class PageConfig(
    @Expose
    var positionIndex: Int? = null,
    @Expose
    var pageName: String = "",
    @Expose
    var pageColor: String = "f",
    @Expose
    var itemStack: String = "",
    @Expose
    var widgets: List<WidgetConfig> = mutableListOf(),
    @Expose
    var sideTabs: List<SidePageConfig> = mutableListOf()) {}

class SidePageConfig(
    @Expose
    var positionIndex: Int? = null,
    @Expose
    var tabName: String = "",
    @Expose
    var tabColor: String = "f",
    @Expose
    var itemStack: String = "",
    @Expose
    var widgets: List<WidgetConfig> = mutableListOf()) {}

class WidgetConfig(
    @Expose
    var widgetId: Int? = null,
    @Expose
    var position: List<Int> = listOf(),
    @Expose
    var shadowText: Boolean = false) {}
