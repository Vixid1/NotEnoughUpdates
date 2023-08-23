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
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11
import java.awt.Color

class ProfileViewerGui(val profile: SkyblockProfiles) : ProfileViewerScreen(profile) {

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        // Stuff to do when page is not the loading page
        // ...
        // Stuff such as player name text box, profile drop down, skycrypt link etc.

        GlStateManager.color(1f, 1f, 1f, 1f)

        // Draw current page
        // ...

        // Draw loading text
        // ...

        // Page ordering or smth
        // ...
        // Read the ordering from config

        // Display any pending tooltip
        // ...

        // Place edit pv button
        // ...
        // Should probably open a new GuiScreen
        // TODO: Make note about resolution being key in order to use edit pv correctly
        Minecraft.getMinecraft().textureManager.bindTexture(pvDropdown)
        Utils.drawTexturedRect((guiLeft - 150).toFloat(), guiTop.toFloat(), 100f, 20f, 0f, 100 / 200f, 0f, 20 / 185f, GL11.GL_NEAREST)
        Utils.drawStringCentered("Edit PV", (guiLeft - 100).toFloat(), (guiTop + 10).toFloat(), true, Color(63, 224, 208).rgb)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (mouseX >= guiLeft - 150 && mouseX <= guiLeft - 50 &&
            mouseY >= guiTop && mouseY <= guiTop + 20) {
            NotEnoughUpdates.profileViewer.loadPlayerByName(playerName) {
                if (it == null) {
                    Utils.addChatMessage(EnumChatFormatting.RED.toString() + "Invalid player name/API key. Maybe the API is down?")
                } else {
                    it.resetCache()
                    NotEnoughUpdates.INSTANCE.openGui = ProfileViewerEditor(it)
                }
            }
        }
    }
}
