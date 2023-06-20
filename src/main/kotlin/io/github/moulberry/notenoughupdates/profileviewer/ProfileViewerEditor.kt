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
import io.github.moulberry.notenoughupdates.core.GlScissorStack
import io.github.moulberry.notenoughupdates.core.GuiElementTextField
import io.github.moulberry.notenoughupdates.core.util.render.RenderUtils
import io.github.moulberry.notenoughupdates.miscgui.GuiInvButtonEditor
import io.github.moulberry.notenoughupdates.profileviewer.widgets.Widgets
import io.github.moulberry.notenoughupdates.util.ScrollBar
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.floor
import kotlin.properties.Delegates

class ProfileViewerEditor(profile: SkyblockProfiles) : ProfileViewerScreen(profile) {

    private val widgetDropdown = ResourceLocation("notenoughupdates:pv_add_widgets.png")
    private val newPageDropdown = ResourceLocation("notenoughupdates:pv_add_new_page.png")
    private val deleteIcon = ResourceLocation("notenoughupdates:core/delete.png")

    private lateinit var scaledRes: ScaledResolution

    private val colors = "0123456789abcdef"

    private var deleteConfirmationMenu = false

    private var newPageMenu = false
    private val newPageNameTextField = GuiElementTextField("", 186, 16, GuiElementTextField.SCALE_TEXT)
    private val newPageIconTextField = GuiElementTextField("", 186, 16, GuiElementTextField.SCALE_TEXT)
    private val newPageMenuScrollBar = ScrollBar()
    private var selectedPageColor = 15

    private var iconStart = 0
    private var iconEnd = 0

    private val searchedIcons: ArrayList<String> = arrayListOf()
    private val searchId: AtomicInteger = AtomicInteger(0)
    private val searchExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val widgetScrollBar = ScrollBar()
    private var widgetMenu = false

    override fun initGui() {
        val widgetMaxSize = (Widgets.values().size - 12) * 20 - 10
        widgetScrollBar.maxScroll = widgetMaxSize
        // Size of scroll bar in texture
        widgetScrollBar.scrollBarHeight = 239
        // Since we want the default scroll bar handle size of 10 we don't need to set it

        searchedIcons.addAll(NotEnoughUpdates.INSTANCE.manager.itemInformation.keys)
        val iconMaxSize = (searchedIcons.size / 8 - 4) * 20
        newPageMenuScrollBar.maxScroll = iconMaxSize
        newPageMenuScrollBar.scrollBarHeight = 111
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        scaledRes = ScaledResolution(Minecraft.getMinecraft())

        Utils.drawStringCentered("Profile Viewer Editor",
            (this.width / 2).toFloat(), (this.height * 0.15).toFloat(), false, Color(255, 128, 32).rgb
        )

        // Preset selector
        for (i in 0 until 3) {
            Utils.drawStringF("> Preset $i" + if (i == selectedPreset) " (§bSelected§f)" else "",
                (guiLeft + sizeX + 50f), (guiTop + i * 20f), false, Color(255, 255, 255).rgb)
        }

        // Add Widget button
        Minecraft.getMinecraft().textureManager.bindTexture(pvDropdown)
        Utils.drawTexturedRect((guiLeft / 2 - 50).toFloat(), (guiTop - 60).toFloat(), 100f, 20f, 0f, 100 / 200f, 0f, 20 / 185f, GL11.GL_NEAREST)
        Utils.drawStringCentered("Add Widget", (guiLeft / 2).toFloat(), (guiTop - 50).toFloat(), true, Color(63, 224, 208).rgb)
        GlStateManager.color(1f, 1f, 1f, 1f)

        // Delete button
        renderDeletePageButton()
        renderDeletePageConfirmation()

        if (mouseX >= guiLeft && mouseX <= (guiLeft + 21) &&
            mouseY >= (guiTop + sizeY + 5) && mouseY <= (guiTop + sizeY + 27)) {
            Utils.drawHoveringText(listOf("§4Delete Page"), mouseX, mouseY, this.width, this.height, 0)
        }

        renderNewPageMenu()
        renderWidgetMenu()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        // Check for preset change
        val textLength = fontRendererObj.getStringWidth("> Preset 3")

        for (i in 0 until 3) {
            if (mouseX >= (guiLeft + sizeX + 50f) && mouseX <= (guiLeft + sizeX + 50f + textLength) &&
                mouseY >= (guiTop + i * 20) && mouseY <= (guiTop + i * 20 + 10)) {
                selectedPreset = i
                selectedPage = 0

                if (!loadPresetData() && !errorDisplayed) {
                    Utils.addChatMessage("§e[NEU] §cThe current profile viewer preset config data could not be found!")
                    errorDisplayed = true
                }

                NotEnoughUpdates.INSTANCE.config.hidden.currentPVPreset = i
                NotEnoughUpdates.INSTANCE.saveConfig()
            }
        }

        // Check for page delete click
        if (mouseX >= guiLeft && mouseX <= (guiLeft + 21) &&
            mouseY >= (guiTop + sizeY + 5) && mouseY <= (guiTop + sizeY + 27)) {
            // presetData.pages.removeAt(selectedPage)
            deleteConfirmationMenu = !deleteConfirmationMenu
        }

        // Check for delete page confirmation menu clicks
        if (deleteConfirmationMenu) {
            // Yes button
            if (mouseX >= (this.width / 2 - 75) && mouseX <= (this.width / 2 - 25) &&
                mouseY >= (this.height / 2 + 15) && mouseY <= (this.height / 2 + 35)) {

                // Remove page from our hashmap and from config
                getCurrentPresetPages().removeAt(selectedPage)
                NotEnoughUpdates.INSTANCE.config.hidden.profileViewerNew["preset_$selectedPreset"]?.pages?.removeAt(selectedPage)

                if (selectedPage > 0) selectedPage -= 1
                deleteConfirmationMenu = !deleteConfirmationMenu
            }

            // No button
            if (mouseX >= (this.width / 2 + 25) && mouseX <= (this.width / 2 + 75) &&
                mouseY >= (this.height / 2 + 15) && mouseY <= (this.height / 2 + 35)) {
                deleteConfirmationMenu = !deleteConfirmationMenu
            }
        }

        // If the new page menu is open check the following
        if (newPageMenu) {
            val newPageMenuX = guiLeft + newPageTabIndex * 28 - 87
            val newPageMenuY = guiTop + 5

            // Check for click on text boxes
            if (mouseX >= newPageMenuX + 7 && mouseX <= newPageMenuX + 7 + newPageNameTextField.width &&
                mouseY >= newPageMenuY + 19 && mouseY <= newPageMenuY + 19 + newPageNameTextField.height) {
                newPageNameTextField.mouseClicked(mouseX, mouseY, mouseButton)
                newPageIconTextField.unfocus()
            }
            if (mouseX >= newPageMenuX + 7 && mouseX <= newPageMenuX + 7 + newPageIconTextField.width &&
                mouseY >= newPageMenuY + 104 && mouseY <= newPageMenuY + 104 + newPageIconTextField.height) {
                newPageIconTextField.mouseClicked(mouseX, mouseY, mouseButton)
                newPageNameTextField.unfocus()
            }

            // Check for click on color option
            for (i in 0 until 16) {
                if (mouseX >= newPageMenuX + 7 + (i % 8 * 20) && mouseX <= newPageMenuX + 25 + (i % 8 * 20) &&
                    mouseY >= newPageMenuY + 50 + floor(i / 8f) * 20 && mouseY <= newPageMenuY + 68 + floor(i / 8f) * 20) {
                    selectedPageColor = i
                }
            }

            // Check for click on an icon
            for (i in iconStart until iconEnd) {
                val iconX = (newPageMenuX + 10 + (i % 8) * 20).toFloat()
                val iconY = newPageMenuY + 133 + floor(i / 8f) * 20 - newPageMenuScrollBar.scroll.value

                if (mouseX >= iconX && mouseX <= iconX + 18 && mouseY >= iconY && mouseY <= iconY + 18) {
                    createNewPage(i)
                }
            }

            // Check for a click outside the menu and close it
            if (!(mouseX >= newPageMenuX && mouseX <= newPageMenuX + 202 &&
                  mouseY >= newPageMenuY && mouseY <= newPageMenuY + 251)) {
                newPageMenu = false
            }
        }

        // Check if new page tab has been clicked
        if (mouseX >= (guiLeft + newPageTabIndex * 28) && mouseX <= (guiLeft + newPageTabIndex * 28 + 28) &&
            mouseY >= (guiTop - 28) && mouseY <= guiTop) {
            newPageMenu = !newPageMenu
        }

        // Check for click on Widget in Add Widget menu
        if (widgetMenu) {
            val widgetMenuX = guiLeft / 2 - 128
            val widgetMenuY = guiTop - 40

            // Check for click in the widget menu first
            if (mouseX >= widgetMenuX && mouseX <= widgetMenuX + 256 &&
                mouseY >= widgetMenuY && mouseY <= widgetMenuY + 256) {

                // Check for click on a widget option
                for (i in 0 until Widgets.values().size) {
                    if (mouseX >= (widgetMenuX + 5) && mouseX <= (widgetMenuX + 246) &&
                        mouseY >= widgetMenuY + 5 + (i * 20) - widgetScrollBar.scroll.value &&
                        mouseY <= widgetMenuY + 23 + (i * 20) - widgetScrollBar.scroll.value) {
                        Utils.addChatMessage("Pressed " + Widgets.values()[i].widgetName)
                    }
                }
            }
        }

        // Check for Add Widget button click
        if (mouseX >= (guiLeft / 2 - 50) && mouseX <= (guiLeft / 2 + 50) &&
            mouseY >= (guiTop - 60) && mouseY <= (guiTop - 40)) {
            widgetMenu = !widgetMenu
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)

        if (newPageMenu && newPageNameTextField.focus) {
            newPageNameTextField.keyTyped(typedChar, keyCode)
        } else if (newPageMenu && newPageIconTextField.focus) {
            val old = newPageIconTextField.text.trim()
            newPageIconTextField.keyTyped(typedChar, keyCode)
            val new = newPageIconTextField.text.trim()

            if (!old.contentEquals(new)) iconSearch()
        }
    }

    override fun handleMouseInput() {
        if (widgetMenu) widgetScrollBar.handleScroll()
        if (newPageMenu) newPageMenuScrollBar.handleScroll()
        super.handleMouseInput()
    }

    override fun onGuiClosed() {
        super.onGuiClosed()

        // Save config on close for any unsaved changes
        NotEnoughUpdates.INSTANCE.saveConfig()
    }

    private fun createNewPage(index: Int) {
        val newPageConfig = PageConfig(
            newPageTabIndex,
            newPageNameTextField.text,
            colors[selectedPageColor].toString(),
            searchedIcons[index]
        )

        // First add new page to our own tracked preset pages
        val newPage = ProfileViewerPage(getCurrentPresetPages().size, newPageConfig)
        getCurrentPresetPages().add(newPage)

        // Second add the new page to the config variable, so we can save the change
        NotEnoughUpdates.INSTANCE.config.hidden.profileViewerNew["preset_$selectedPreset"]?.pages?.add(newPageConfig)

        NotEnoughUpdates.INSTANCE.saveConfig()
    }

    private fun renderDeletePageButton() {
        Minecraft.getMinecraft().textureManager.bindTexture(pvDropdown)
        Utils.drawTexturedRect(guiLeft.toFloat(), (guiTop + sizeY + 5f), 21f, 22f, 0f, 21 / 200f, 20 / 185f, 42 / 185f, GL11.GL_NEAREST)
        GlStateManager.color(1f, 1f, 1f, 1f)

        Minecraft.getMinecraft().textureManager.bindTexture(deleteIcon)
        Utils.drawTexturedRect((guiLeft + 5f), (guiTop + sizeY + 9f), 11f, 14f, 0f, 1f, 0f, 1f, GL11.GL_NEAREST)
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun renderDeletePageConfirmation() {
        if (!deleteConfirmationMenu) return

        val pageName = getCurrentPresetPages()[selectedPage].pageName

        RenderUtils.drawFloatingRectDark((this.width / 2 - 100), (this.height / 2 - 50), 200, 100)
        Utils.drawStringCentered("Delete Page?", (this.width / 2f), (this.height / 2f - 40), false, Color.RED.rgb)
        Utils.drawStringCentered("Are you sure you want", (this.width / 2f), (this.height / 2f - 15), false, Color.WHITE.rgb)
        Utils.drawStringCentered("to delete page: $pageName?", (this.width / 2f), (this.height / 2f - 5), false, Color.WHITE.rgb)
        RenderUtils.drawFloatingRectDark((this.width / 2 - 75), (this.height / 2 + 15), 50, 20)
        RenderUtils.drawFloatingRectDark((this.width / 2 + 25), (this.height / 2 + 15), 50, 20)
        Utils.drawStringCentered("Yes", (this.width / 2f - 50), (this.height / 2f + 25), false, Color.RED.rgb)
        Utils.drawStringCentered("No", (this.width / 2f + 50), (this.height / 2f + 25), false, Color.GREEN.rgb)
    }

    private fun renderNewPageMenu() {
        if (!newPageMenu) return

        val mouseX = Mouse.getX() * (this.width.toFloat() / Minecraft.getMinecraft().displayWidth.toFloat())
        val mouseY = this.height - Mouse.getY() * (this.height.toFloat() / Minecraft.getMinecraft().displayHeight.toFloat())

        val x = guiLeft + newPageTabIndex * 28 - 87
        val y = guiTop + 5

        GlStateManager.color(1f, 1f, 1f, 1f)

        Minecraft.getMinecraft().textureManager.bindTexture(newPageDropdown)
        Utils.drawTexturedRect(x.toFloat(), y.toFloat(), 202f, 251f, 0f, 202f / 256f, 5f / 256f, 1f, GL11.GL_NEAREST)

        // Draw little arrow
        Minecraft.getMinecraft().textureManager.bindTexture(newPageDropdown)
        Utils.drawTexturedRect(x + 98f, y - 2f, 10f, 5f, 0f, 6f / 256f, 0f, 5f / 256f, GL11.GL_NEAREST)

        Utils.drawStringF("Page Name", x + 7f, y + 7f, false, -0x5f5f60)
        newPageNameTextField.render(x + 7, y + 19)

        Utils.drawStringF("Page Color", x + 7f, y + 40f, false, -0x5f5f60)
        for (i in 0 until 16) {
            if (i == selectedPageColor) {
                drawRect(x + 6 + (i % 8 * 20), (y + 49 + floor(i / 8f) * 20).toInt(), x + 26 + (i % 8 * 20),
                    (y + 69 + floor(i / 8f) * 20).toInt(), -0xffff01)
            }

            Minecraft.getMinecraft().textureManager.bindTexture(newPageDropdown)
            GlStateManager.color(1f, 1f, 1f, 1f)
            Utils.drawTexturedRect(x + 7f + (i % 8 * 20), y + 50f + floor(i / 8f) * 20, 18f, 18f, 202f / 256f, 220f / 256f, 0f, 18f / 256f, GL11.GL_NEAREST)
            Utils.drawStringF(EnumChatFormatting.values()[i].toString() + colors[i], x + 13f + (i % 8 * 20), y + 55f + floor(i / 8f) * 20, false, 0)
        }

        Utils.drawStringF("Icon Selector", x + 7f, y + 92f, false, -0x5f5f60)
        newPageIconTextField.render(x + 7, y + 104)

        newPageMenuScrollBar.tick()
        drawRect(x + 184, (y + 129 + newPageMenuScrollBar.scrollY).toInt(), x + 190, (y + 129 + newPageMenuScrollBar.scrollY + newPageMenuScrollBar.scrollBarHandleSize).toInt(), -0xdfdfe0)

        GlScissorStack.push(x + 10, y + 127, x + 179, y + 240, scaledRes)

        val tooltipToRender: ArrayList<String> = arrayListOf()

        synchronized (searchedIcons) {
            // Can see at most 56 icons
            iconStart = newPageMenuScrollBar.scroll.value / 20 * 8
            iconEnd = searchedIcons.size
            if (iconStart < 0) iconStart = 0
            if (iconEnd > iconStart + 56) iconEnd = iconStart + 56

            for (i in iconStart until iconEnd) {
                val iconX = (x + 10 + (i % 8) * 20).toFloat()
                val iconY = y + 133 + floor(i / 8f) * 20 - newPageMenuScrollBar.scroll.value

                Minecraft.getMinecraft().textureManager.bindTexture(newPageDropdown)
                GlStateManager.color(1f, 1f, 1f, 1f)
                Utils.drawTexturedRect(iconX, iconY, 18f, 18f, 202f / 256f, 220f / 256f, 0f, 18f / 256f, GL11.GL_NEAREST)

                val iconString = searchedIcons[i]
                val stack = renderIcon(iconString, iconX + 1, iconY + 1)

                if (mouseX >= iconX && mouseX <= iconX + 18 && mouseY >= iconY && mouseY <= iconY + 18) {
                    tooltipToRender.add(stack.getTooltip(Minecraft.getMinecraft().thePlayer, false)[0])
                }
            }
        }

        GlScissorStack.pop(scaledRes)

        // Need to render tooltip outside scissor
        Utils.drawHoveringText(tooltipToRender, mouseX.toInt(), mouseY.toInt(), width, height, 0)
    }

    private fun renderIcon(icon: String, x: Float, y: Float) : ItemStack {
        val stack = GuiInvButtonEditor.getStack(icon)

        var scale = 1f
        if (icon.startsWith("skull:")) scale = 1.2f

        GlStateManager.pushMatrix()
        GlStateManager.translate(x + 8f, y + 8f, 0f)
        GlStateManager.scale(scale, scale, 1f)
        GlStateManager.translate(-8f, -8f, 0f)
        Utils.drawItemStack(stack, 0, 0)
        GlStateManager.popMatrix()

        return stack
    }

    private fun renderWidgetMenu() {
        if (!widgetMenu) return

        val x = guiLeft / 2 - 128
        val y = guiTop - 40

        GlStateManager.color(1f, 1f, 1f, 1f)

        Minecraft.getMinecraft().textureManager.bindTexture(widgetDropdown)
        Utils.drawTexturedRect(x.toFloat(), y.toFloat(), 256f, 256f, GL11.GL_NEAREST)

        widgetScrollBar.tick()

        // Scroll bar handle
        drawRect(x + 242, (y + 9 + widgetScrollBar.scrollY).toInt(), x + 248, (y + 9 + widgetScrollBar.scrollY + widgetScrollBar.scrollBarHandleSize).toInt(), -0xdfdfe0)

        GlScissorStack.push(x + 5, y + 5, x + 234, y + 251, scaledRes)

        for (i in 0 until Widgets.values().size) {
            Utils.drawStringF("$i - " + Widgets.values()[i].widgetName, (x + 7f), (y + 10f + (i * 20) - widgetScrollBar.scroll.value), false, Color(255, 255, 255).rgb)
        }

        handleWidgetMenuHover(x, y)

        GlScissorStack.pop(scaledRes)
    }

    private fun handleWidgetMenuHover(x: Int, y: Int) {
        val mouseX = Mouse.getX() * (this.width.toFloat() / Minecraft.getMinecraft().displayWidth.toFloat())
        val mouseY = this.height - Mouse.getY() * (this.height.toFloat() / Minecraft.getMinecraft().displayHeight.toFloat())

        for (i in 0 until Widgets.values().size) {
            if (mouseX >= (x + 5) && mouseX <= (x + 246) &&
                mouseY >= y + 5 + (i * 20) - widgetScrollBar.scroll.value &&
                mouseY <= y + 23 + (i * 20) - widgetScrollBar.scroll.value) {
                drawRect(x + 5,
                    y + 5 + (i * 20) - widgetScrollBar.scroll.value,
                    x + 238,
                    y + 23 + (i * 20) - widgetScrollBar.scroll.value,
                    Color(200, 200, 200, 128).rgb)
            }
        }
    }

    private fun iconSearch() {
        val thisSearchId = searchId.incrementAndGet()
        val searchString = newPageIconTextField.text

        if (searchString.trim().isEmpty()) {
            synchronized (searchedIcons) {
                searchedIcons.clear()
                searchedIcons.addAll(NotEnoughUpdates.INSTANCE.manager.itemInformation.keys)
            }
            return
        }

        searchExecutor.submit {
            if (thisSearchId != searchId.get()) return@submit

            val title: MutableSet<String> = NotEnoughUpdates.INSTANCE.manager.search("title:" + searchString.trim())

            if (thisSearchId != searchId.get()) return@submit

            if (!searchString.trim().contains(" ")) {
                val sb = StringBuilder()
                for (c in searchString.toCharArray()) {
                    sb.append(c).append(" ")
                }
                title.addAll(NotEnoughUpdates.INSTANCE.manager.search("title:" + sb.toString().trim()))
            }

            if (thisSearchId != searchId.get()) return@submit

            val desc: MutableSet<String> = NotEnoughUpdates.INSTANCE.manager.search("desc:" + searchString.trim())
            desc.removeAll(title)

            if (thisSearchId != searchId.get()) return@submit

            synchronized (searchedIcons) {
                searchedIcons.clear()
                searchedIcons.addAll(title)
                searchedIcons.addAll(desc)

                // Set scroll back to the top after every search
                newPageMenuScrollBar.scroll.value = 0

                // Update maxScroll of icon scroll bar after search
                var max = (searchedIcons.size / 8 - 4) * 20
                if (max < 0) max = 0
                newPageMenuScrollBar.maxScroll = max
            }
        }
    }
}
