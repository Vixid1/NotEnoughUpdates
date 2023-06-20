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
import io.github.moulberry.notenoughupdates.profileviewer.widgets.misc.PlayerModelWidget
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.Shader
import net.minecraft.util.Matrix4f
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color

abstract class ProfileViewerScreen(val profile: SkyblockProfiles) : GuiScreen() {

    protected val pvBg = ResourceLocation("notenoughupdates:pv_bg.png")
    protected val pvDropdown = ResourceLocation("notenoughupdates:pv_dropdown.png")
    protected val pvElements = ResourceLocation("notenoughupdates:pv_elements.png")

    protected val sizeX = 431
    protected val sizeY = 202

    protected var newPageTabIndex: Int = 0

    protected var errorDisplayed = false

    private var loading = false

    private lateinit var blurOutputHorizontal: Framebuffer
    private lateinit var blurOutputVertical: Framebuffer
    private lateinit var blurShaderHorizontal: Shader
    private lateinit var blurShaderVertical: Shader
    private var lastBgBlurFactor: Int = -1

    init {
        Companion.profile = profile
        profileName = profile.latestProfileName
    }

    companion object {
        var currentTime: Long = 0
        var startTime: Long = 0
        var lastTime: Long = 0

        var guiLeft: Int = 0
        var guiTop: Int = 0

        var profile: SkyblockProfiles? = null
        var profileName: String = ""

        var selectedPage: Int = 0
        var presetData: HashMap<String, MutableList<ProfileViewerPage>> = hashMapOf()

        var selectedPreset: Int = 0

        fun getSelectedProfile() : SkyblockProfiles.SkyblockProfile? = profile?.getProfile(profileName)

        fun getEntityPlayerOrNull() : EntityOtherPlayerMP? {
            val curPreset = presetData["preset_$selectedPreset"] ?: return null
            if (curPreset[selectedPage].widgets.any { it is PlayerModelWidget }) {
               return curPreset[selectedPage].widgets.filterIsInstance<PlayerModelWidget>()[0].entityPlayer
            }
            return null
        }
    }

    private fun drawBlurredBackground() {
        drawDefaultBackground()
        blurBackground()
        renderBlurredBackground(this.width, this.height, guiLeft + 2, guiTop + 2, sizeX -4, sizeY - 4)
    }
    
    private fun blurBackground() {
        if (!OpenGlHelper.isFramebufferEnabled()) return

        val width = Minecraft.getMinecraft().displayWidth
        val height = Minecraft.getMinecraft().displayHeight

        if (!this::blurOutputHorizontal.isInitialized) {
            blurOutputHorizontal = Framebuffer(width, height, false)
            blurOutputHorizontal.setFramebufferFilter(GL11.GL_NEAREST)
        }

        if (!this::blurOutputVertical.isInitialized) {
            blurOutputVertical = Framebuffer(width, height, false)
            blurOutputVertical.setFramebufferFilter(GL11.GL_NEAREST)
        }

        if (blurOutputHorizontal.framebufferWidth != width || blurOutputHorizontal.framebufferHeight != height) {
            blurOutputHorizontal.createBindFramebuffer(width, height)
            blurShaderHorizontal.setProjectionMatrix(createProjectionMatrix(width, height))
            Minecraft.getMinecraft().framebuffer.bindFramebuffer(false)
        }

        if (blurOutputVertical.framebufferWidth != width || blurOutputVertical.framebufferHeight != height) {
            blurOutputVertical.createBindFramebuffer(width, height)
            blurShaderVertical.setProjectionMatrix(createProjectionMatrix(width, height))
            Minecraft.getMinecraft().framebuffer.bindFramebuffer(false)
        }

        if (!this::blurShaderHorizontal.isInitialized) {
            try {
                blurShaderHorizontal = Shader(
                    Minecraft.getMinecraft().resourceManager,
                    "blur",
                    Minecraft.getMinecraft().framebuffer,
                    blurOutputHorizontal
                )
                blurShaderHorizontal.shaderManager.getShaderUniform("BlurDir").set(1F, 0F)
                blurShaderHorizontal.setProjectionMatrix(createProjectionMatrix(width, height))
            } catch (ignored: Exception) {}
        }

        if (!this::blurShaderVertical.isInitialized) {
            try {
                blurShaderVertical = Shader(
                    Minecraft.getMinecraft().resourceManager,
                    "blur",
                    blurOutputHorizontal,
                    blurOutputVertical
                )
                blurShaderVertical.shaderManager.getShaderUniform("BlurDir").set(0F, 1F)
                blurShaderVertical.setProjectionMatrix(createProjectionMatrix(width, height))
            } catch (ignored: Exception) {}
        }

        if (this::blurShaderHorizontal.isInitialized && this::blurShaderVertical.isInitialized) {
            if (lastBgBlurFactor != 15) {
                blurShaderHorizontal.shaderManager.getShaderUniform("Radius").set(15F)
                blurShaderVertical.shaderManager.getShaderUniform("Radius").set(15F)
                lastBgBlurFactor = 15
            }
            GlStateManager.pushMatrix()
            blurShaderHorizontal.loadShader(0F)
            blurShaderVertical.loadShader(0F)
            GlStateManager.enableDepth()
            GlStateManager.popMatrix()

            Minecraft.getMinecraft().framebuffer.bindFramebuffer(false)
        }
    }

    /**
     * Creates a projection matrix that projects from our coordinate space [0->width; 0->height] to OpenGL coordinate
     * space [-1 -> 1; 1 -> -1] (Note: flipped y-axis).
     *
     * This is so that we can render to and from the framebuffer in a way that is familiar to us, instead of needing to
     * apply scales and translations manually.
     *
     */
    fun createProjectionMatrix(width: Int, height: Int) : Matrix4f {
        val projMatrix = Matrix4f()
        projMatrix.setIdentity()
        projMatrix.m00 = 2.0f / width.toFloat()
        projMatrix.m11 = 2.0f / (-height).toFloat()
        projMatrix.m22 = -0.0020001999f
        projMatrix.m33 = 1.0f
        projMatrix.m03 = -1.0f
        projMatrix.m13 = 1.0f
        projMatrix.m23 = -1.0001999f
        return projMatrix
    }

    fun renderBlurredBackground(width: Int, height: Int, x: Int, y: Int, blurWidth: Int, blurHeight: Int) {
        if (!OpenGlHelper.isFramebufferEnabled()) return

        val uMin = x / width.toFloat()
        val uMax = (x + blurWidth) / width.toFloat()
        val vMin = (height - y) / height.toFloat()
        val vMax = (height - y - blurHeight) / height.toFloat()

        // In case this method gets called before blurBackground for whatever reason
        if (!this::blurOutputVertical.isInitialized) blurBackground()

        blurOutputVertical.bindFramebufferTexture()
        GlStateManager.color(1f, 1f, 1f, 1f)
        Utils.drawTexturedRect(x.toFloat(), y.toFloat(), blurWidth.toFloat(), blurHeight.toFloat(), uMin, uMax, vMin, vMax)
        blurOutputVertical.unbindFramebufferTexture()
    }

    override fun initGui() {
        super.initGui()

        // Get selected preset
        selectedPreset = NotEnoughUpdates.INSTANCE.config.hidden.currentPVPreset

        if (!loadPresetData() && !errorDisplayed) {
            Utils.addChatMessage("§e[NEU] §cThe current profile viewer preset config data could not be found!")
            errorDisplayed = true
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        if (profile.getOrLoadSkyblockProfiles(null) == null) {
            loading = true
        }

        currentTime = System.currentTimeMillis()
        if (startTime == 0L) startTime = currentTime

        lastTime = currentTime

        guiLeft = (this.width - this.sizeX) / 2
        guiTop = (this.height - this.sizeY) / 2

        // Draw the default background and blurred background for the pv
        drawBlurredBackground()

        renderCurrentPreset(mouseX, mouseY)

        // Page tooltips
        val tooltipToRender: ArrayList<String> = arrayListOf()
        val currentPresetPages = presetData["preset_$selectedPreset"] ?: return
        for (i in currentPresetPages.indices) {
            if (mouseX >= (guiLeft + i * 28) && mouseX <= (guiLeft + i * 28 + 28) &&
                mouseY >= (guiTop - 28) && mouseY <= guiTop) {
                tooltipToRender.add("§" + currentPresetPages[i].pageColor + currentPresetPages[i].pageName)
            }
        }
        Utils.drawHoveringText(tooltipToRender, mouseX, mouseY, this.width, this.height, 0)
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        val currentPresetPages = presetData["preset_$selectedPreset"] ?: return

        // Check for page change
        for (i in currentPresetPages.indices) {
            if (mouseX > (guiLeft + i * 28) && mouseX < (guiLeft + i * 28 + 28) &&
                mouseY >= (guiTop - 28) && mouseY <= guiTop) {
                selectedPage = i
            }
        }

        // Run mouseClicked in current page widgets
        for (widgets in currentPresetPages[selectedPage].widgets) {
            widgets.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    open fun renderCurrentPreset(mouseX: Int, mouseY: Int) {
        val pages = getCurrentPresetPages()

        // Page tabs
        for (i in pages.indices) {

            val x: Int = guiLeft + i * 28
            val y: Int = guiTop - 28

            val stack = pages[i].tabItemStack

            renderTab(i, x, y) {
                GlStateManager.enableDepth()
                Utils.drawItemStack(stack, x + 6, y + 9)
            }
        }

        // Add new page tab
        if (Minecraft.getMinecraft().currentScreen is ProfileViewerEditor) {
            newPageTabIndex = pages.size

            val x: Int = guiLeft + newPageTabIndex * 28
            val y: Int = guiTop - 28

            renderTab(newPageTabIndex, x, y) {
                // Probably better to hardcode this into the elements.png file since
                // I am overhauling the entire pv layout :skull:
                drawRect(x + 14, y + 12, x + 16, y + 22, Color(255, 255, 255).rgb)
                drawRect(x + 10, y + 16, x + 20, y + 18, Color(255, 255, 255).rgb)
            }
        }

        Minecraft.getMinecraft().textureManager.bindTexture(pvBg)
        Utils.drawTexturedRect(guiLeft.toFloat(), guiTop.toFloat(), sizeX.toFloat(), sizeY.toFloat(), GL11.GL_NEAREST)

        // Widgets for selected page
        if (pages.size > 0) {
            for (widget in pages[selectedPage].widgets) {
                GlStateManager.color(1f, 1f, 1f, 1f)
                widget.render(mouseX, mouseY)
            }
        }
    }

    private fun renderTab(i: Int, x: Int, y: Int, runnable: Runnable) {
        GlStateManager.pushMatrix()

        GlStateManager.disableDepth()
        GlStateManager.translate(0.0, 0.0, -2.0)

        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        )
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(516, 0.1f)

        var uMin = 0 / 256f
        var uMax = 28 / 256f
        var vMin = 20 / 256f
        var vMax = 51 / 256f

        if (i == selectedPage) {
            GlStateManager.enableDepth()
            GlStateManager.translate(0.0, 0.0, 5.0)
            vMin = 52 / 256f
            vMax = 84 / 256f

            if (i != 0) {
                uMin = 28 / 256f
                uMax = 56 / 256f
            }
        }

        renderBlurredBackground(width, height, x + 2, y + 2, 24, 24)

        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GL14.glBlendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ONE_MINUS_SRC_ALPHA
        )
        GlStateManager.enableAlpha()
        GlStateManager.alphaFunc(516, 0.1f)

        Minecraft.getMinecraft().textureManager.bindTexture(pvElements)
        Utils.drawTexturedRect(x.toFloat(), y.toFloat(), 28f, if (i == selectedPage) 32f else 31f, uMin, uMax, vMin, vMax, GL11.GL_NEAREST)

        runnable.run()

        GlStateManager.enableDepth()
        GlStateManager.popMatrix()
    }

    /**
     * @return false if preset was not found in config, else true
     */
    protected fun loadPresetData() : Boolean {
        val presetConfigData = NotEnoughUpdates.INSTANCE.config.hidden.profileViewerNew["preset_$selectedPreset"] ?: return false

        presetData.computeIfAbsent("preset_$selectedPreset") {
            val presetPages = mutableListOf<ProfileViewerPage>()

            // Create a ProfileViewerPage for each page in the current preset
            for (i in presetConfigData.pages.indices) {
                presetPages.add(ProfileViewerPage(i, presetConfigData.pages[i]))
            }

            return@computeIfAbsent presetPages
        }

        return true
    }

    protected fun getCurrentPresetPages() : MutableList<ProfileViewerPage> {
        return presetData["preset_$selectedPreset"] ?: return mutableListOf()
    }

    override fun onGuiClosed() {
        super.onGuiClosed()

        val pages = getCurrentPresetPages()

        if (pages.size > 0) {
            for (page in pages) {
                for (widget in page.widgets) {
                    widget.resetCache()
                }
            }
        }
    }

    override fun doesGuiPauseGame() = false
}
