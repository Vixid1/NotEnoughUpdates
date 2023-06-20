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

package io.github.moulberry.notenoughupdates.util

import io.github.moulberry.notenoughupdates.core.util.lerp.LerpingInteger
import org.lwjgl.input.Mouse
import kotlin.math.abs
import kotlin.math.floor

/**
 * Class to deal with the logic of a Scroll Bar, no rendering of the scroll
 * bar is done in this class and all rendering must be done separately.
 */
class ScrollBar(
    /**
     * Maximum amount of pixels to scroll
     */
    var maxScroll: Int,
    /**
     * The height that the scroll bar handle moves through
     */
    var scrollBarHeight: Int,
    /**
     * The size (height) of the scroll bar handle (default = 10)
     */
    var scrollBarHandleSize: Int = 10
) {

    /**
     * Using the parameterless constructor should only be used to initially
     * create an instance of a scroll bar, the parameters should then be
     * manually set before use
     */
    constructor() : this(0, 0)

    /**
     * Access scroll.value to move elements when scrolling
     */
    var scroll = LerpingInteger(0, 100)
    private var currentScroll = 0
    /**
     * Access this value to render the position of the scroll bar handle
     */
    var scrollY = 0f

    /**
     * Should be called every frame in the screen with the current scroll bar
     */
    fun tick() {
        currentScroll = scroll.value
        if (currentScroll > maxScroll) currentScroll = maxScroll

        scrollY = floor((scrollBarHeight - scrollBarHandleSize) * (currentScroll.toFloat() / maxScroll.toFloat()))

        scroll.tick()
    }

    /**
     * This should typically be called in the overridden GuiScreen#handleMouseInput() method
     */
    fun handleScroll() {
        var scrollAmount = Mouse.getEventDWheel()

        if (scrollAmount != 0) {
            scrollAmount = if (scrollAmount > 0) -12 else 12

            val delta = abs(scroll.target - scroll.value)
            val acc = delta / 20 + 1
            scrollAmount *= acc

            var newTarget: Int = scroll.target + scrollAmount

            if (newTarget > maxScroll) newTarget = maxScroll
            if (newTarget < 0) newTarget = 0

            scroll.target = newTarget
            scroll.resetTimer()
        }
    }
}
