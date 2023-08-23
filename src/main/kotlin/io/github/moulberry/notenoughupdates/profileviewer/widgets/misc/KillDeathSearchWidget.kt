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

package io.github.moulberry.notenoughupdates.profileviewer.widgets.misc

import io.github.moulberry.notenoughupdates.core.config.Position
import io.github.moulberry.notenoughupdates.profileviewer.widgets.WidgetInterface

class KillDeathSearchWidget(
    override val widgetName: String,
    override var position: Position,
    override val shadowText: Boolean,
    override var size: MutableList<Int>
) : WidgetInterface {

    override fun render(mouseX: Int, mouseY: Int) {
        TODO("Not yet implemented")
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        TODO("Not yet implemented")
    }

    override fun resetCache() {}

}
