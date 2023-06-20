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

import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.profileviewer.widgets.WidgetInterface
import io.github.moulberry.notenoughupdates.profileviewer.widgets.Widgets
import net.minecraft.item.ItemStack
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

class ProfileViewerPage(val pageIndex: Int, pageConfig: PageConfig) {

    val pageName: String = pageConfig.pageName
    val pageColor: String = pageConfig.pageColor
    val tabItemStack: ItemStack = NotEnoughUpdates.INSTANCE.manager.createItem(pageConfig.itemStack)
    val widgets = mutableListOf<WidgetInterface>()

    init {
        for (each in pageConfig.widgets) {
            Widgets.getWidget(each.widgetId)?.let { createPageWidgets(it, each.position, each.shadowText) }
        }
    }

    private fun createPageWidgets(widget: Widgets, pos: List<Int>, shadowText: Boolean) {
        val clazz = widget.classDef

        val primaryConstructor = clazz.primaryConstructor ?: return
        val paramFields = primaryConstructor.parameters
        val args = mutableMapOf<KParameter, Any>()

        // Widget name
        args[paramFields[0]] = widget.widgetName
        // Widget position
        args[paramFields[1]] = pos
        // Shadow text
        args[paramFields[2]] = shadowText

        widgets.add(primaryConstructor.callBy(args))
    }
}
