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

import io.github.moulberry.notenoughupdates.core.config.Position

interface WidgetInterface {

    /**
     * Friendly name of the widget
     */
    val widgetName: String
    /**
     * Current x, y offset from guiLeft and guiTop respectively for widget position
     */
    var position: Position
    /**
     * Whether to render text in this widget with shadows or not (not all widgets
     * have text so this will do nothing for some widgets)
     */
    val shadowText: Boolean
    /**
     * Size of the widget, mostly used for moving the widget in editor screen.
     */
    var size: MutableList<Int>

    fun render(mouseX: Int, mouseY: Int)

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)

    fun resetCache()

}
