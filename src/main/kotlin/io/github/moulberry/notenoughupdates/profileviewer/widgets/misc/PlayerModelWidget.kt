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

import com.google.gson.JsonObject
import com.mojang.authlib.GameProfile
import com.mojang.authlib.minecraft.MinecraftProfileTexture
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.core.config.Position
import io.github.moulberry.notenoughupdates.profileviewer.Panorama
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.currentTime
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.getSelectedProfile
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.profile
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.startTime
import io.github.moulberry.notenoughupdates.profileviewer.SkyblockProfiles
import io.github.moulberry.notenoughupdates.profileviewer.widgets.WidgetInterface
import io.github.moulberry.notenoughupdates.util.*
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.EntityLivingBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import kotlin.math.atan
import kotlin.math.sin

class PlayerModelWidget(
    override val widgetName: String,
    override var position: Position,
    override val shadowText: Boolean,
    override var size: MutableList<Int> = mutableListOf(90, 130)
) : WidgetInterface {
    
    private lateinit var scaledRes: ScaledResolution

    private var selectedProfile: SkyblockProfiles.SkyblockProfile? = getSelectedProfile()

    private val panoramaOverlay = ResourceLocation("notenoughupdates:pv_panorama_overlay.png")

    private var panoramaClickedX: Int = -1
    private var panoramaRotation: Float = 0f

    private var pronouns: AsyncDependencyLoader<Optional<PronounDB.PronounChoice>> = AsyncDependencyLoader.withEqualsInvocation({
        if (NotEnoughUpdates.INSTANCE.config.profileViewer.showPronounsInPv)
            Optional.ofNullable(profile).map { Utils.parseDashlessUUID(it.uuid) }
        else
            Optional.empty() },
        {uuid ->
            if (uuid.isPresent)
                PronounDB.getPronounsFor(uuid.get())
            else
                CompletableFuture.completedFuture(Optional.empty())})

    var entityPlayer: EntityOtherPlayerMP? = null
    private var profileLoader = Executors.newFixedThreadPool(1)
    private var playerLocationSkin: ResourceLocation? = null
    private var playerLocationCape: ResourceLocation? = null
    private var entitySkinType: String? = null
    private var loadingProfile = false

    override fun render(mouseX: Int, mouseY: Int) {
        scaledRes = ScaledResolution(Minecraft.getMinecraft())
        
        val width = Minecraft.getMinecraft().displayWidth
        val height = Minecraft.getMinecraft().displayHeight

        val posX = position.getAbsX(scaledRes, size[0])
        val posY = position.getAbsY(scaledRes, size[1])

        if (selectedProfile == null) selectedProfile = getSelectedProfile()

        // Get player location
        var location = ""
        val status = profile?.playerStatus
        if (status != null && status.has("mode")) location = status.get("mode").asString

        // Panorama rotation
        var extraRotation = 0

        if (Mouse.isButtonDown(0) || Mouse.isButtonDown(1)) {
            if (panoramaClickedX == -1) {
                if (mouseX > posX + 5 && mouseY < posX + 86 &&
                    mouseY > posY + 5 && mouseY < posY + 113) {
                    panoramaClickedX = mouseX
                }
            }
        } else {
            if (panoramaClickedX != -1) {
                panoramaRotation += mouseX - panoramaClickedX
                panoramaClickedX = -1
            }
        }

        if (panoramaClickedX == -1) {
            panoramaRotation += (currentTime - ProfileViewerScreen.lastTime) / 400f
        } else {
            extraRotation = mouseX - panoramaClickedX
        }
        panoramaRotation %= 360

        // Render Panorama
        var panoramaIdentifier = "day"
        if (SBInfo.getInstance().currentTimeDate != null) {
            if (SBInfo.getInstance().currentTimeDate.hours <= 6 || SBInfo.getInstance().currentTimeDate.hours >= 20) {
                panoramaIdentifier = "night"
            }
        }

        Panorama.drawPanorama(
            (-panoramaRotation - extraRotation), posX + 5, posY + 5,
            81, 108, 0.37f, 0.8f,
            Panorama.getPanoramasForLocation((location.ifBlank { "unknown" }), panoramaIdentifier))

        // Pronoun hover
        if (mouseX >= posX + 5 && mouseX <= posX + 86 &&
            mouseY >= posY + 5 && mouseY <= posY + 113) {
            val pronounChoice = pronouns.peekValue().flatMap { it }
            if (pronounChoice.isPresent) {
                val pronoun = pronounChoice.get()
                if (pronoun.isConsciousChoice) {
                    Utils.drawHoveringText(pronoun.render().map { "§7$it" }, mouseX, mouseY, width, height, -1)
                }
            }
        }

        // Panorama background / overlay
        Minecraft.getMinecraft().textureManager.bindTexture(panoramaOverlay)
        Utils.drawTexturedRect(posX + 5f, posY + 5f, 81f, 108f, 0f, 1f, 0f, 1f, GL11.GL_NEAREST)

        loadPlayerIntoEntity()

        if (entityPlayer != null) {
            loadAndRenderPet()

            drawNametag()
            drawStatus(status, location)

            entityPlayer?.let { renderEntity(posX + 45, posY + 95, 36, posX + 45 - mouseX, posY + 96 - mouseY, it) }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
    }

    private fun drawNametag() {
        val hypixelProfile = profile?.hypixelProfile

        if (hypixelProfile != null) {
            var playerName = ""

            if (hypixelProfile.has("prefix")) {
                playerName = Utils.getElementAsString(hypixelProfile.get("prefix"), "") + " " + entityPlayer?.name
            } else {
                var rank = Utils.getElementAsString(
                    hypixelProfile.get("rank"),
                    Utils.getElementAsString(hypixelProfile.get("newPackageRank"), "NONE"))

                val monthlyPackageRank = Utils.getElementAsString(hypixelProfile.get("monthlyPackageRank"), "NONE")

                if (!rank.equals("YOUTUBER") && !monthlyPackageRank.equals("NONE")) rank = monthlyPackageRank

                val rankPlusColorECF = EnumChatFormatting.getValueByName(
                    Utils.getElementAsString(hypixelProfile.get("rankPlusColor"), "GOLD"))

                var rankPlusColor = EnumChatFormatting.GOLD.toString()
                if (rankPlusColorECF != null) rankPlusColor = rankPlusColorECF.toString()

                val misc = Constants.MISC
                if (misc != null) {
                    if (misc.has("ranks")) {
                        val rankName = Utils.getElementAsString(Utils.getElement(misc, "ranks.$rank.tag"), null)
                        val rankColor = Utils.getElementAsString(Utils.getElement(misc, "ranks.$rank.color"), "7")
                        val rankPlus = Utils.getElementAsString(Utils.getElement(misc, "ranks.$rank.plus"), "")

                        var name = entityPlayer?.name

                        if (misc.has("special_bois")) {
                            val specialBois = misc.get("special_bois").asJsonArray

                            for (i in 0 until specialBois.size()) {
                                if (specialBois.get(i).asString.equals(profile?.uuid)) {
                                    name = Utils.chromaString(name)
                                    break
                                }
                            }
                        }

                        playerName = EnumChatFormatting.GRAY.toString() + name
                        if (rankName != null) {
                            val icon = if (selectedProfile?.gamemode != null) getIcon(selectedProfile?.gamemode) else ""

                            playerName = "§$rankColor[$rankName$rankPlusColor$rankPlus§$rankColor] $name" +
                                    (if (icon == "") "" else " $icon")
                        }
                    }
                }
            }

            if (playerName != "") {
                val rankPrefixLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(playerName)
                val halfRankPrefixLen = rankPrefixLen / 2

                val x = position.getAbsX(scaledRes, size[0]) + (size[0] / 2)
                val y = position.getAbsY(scaledRes, size[1]) + 15

                GuiScreen.drawRect(
                    x - halfRankPrefixLen - 1,
                    y - 1,
                    x + halfRankPrefixLen + 1,
                    y + 8,
                    Color(0, 0, 0, 64).rgb
                )

                GlStateManager.color(1f, 1f, 1f, 1f)
                Minecraft.getMinecraft().fontRendererObj.drawString(playerName,
                    (x - halfRankPrefixLen).toFloat(), y.toFloat(), 0, true)
            }
        }
    }

    private fun getIcon(gamemodeType: String?) : String {
        return when (gamemodeType) {
            "island" -> "§a☀"
            "bingo" -> "§7Ⓑ"
            "ironman" -> "§7♲"
            else -> ""
        }
    }

    private fun drawStatus(status: JsonObject?, location: String) {
        if (status != null) {
            val onlineElement = Utils.getElement(status, "online")

            val online = (onlineElement != null) && onlineElement.isJsonPrimitive && onlineElement.asBoolean

            var statusStr = if (online) EnumChatFormatting.GREEN.toString() + "ONLINE"
                            else EnumChatFormatting.RED.toString() + "OFFLINE"

            var locationStr = ""

            if (profile?.uuid.equals("20934ef9488c465180a78f861586b4cf")) {
                locationStr = "Ignoring DMs"
            } else if (online) {
                locationStr = NotEnoughUpdates.INSTANCE.navigation.getNameForAreaModeOrUnknown(location)
            }
            // No point in implementing the Technoblade check since none of his profiles have "selected": true
            // so the pv will always assume "No SkyBlock data found!" :(

            if (locationStr != "") {
                statusStr += EnumChatFormatting.GRAY.toString() + " - " + EnumChatFormatting.GREEN.toString() + locationStr
            }

            Utils.drawStringCentered(statusStr, (position.getAbsX(scaledRes, size[0]) + size[0] / 2).toFloat(), (position.getAbsY(scaledRes, size[1]) + 122).toFloat(), true, 0)
        }
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun loadPlayerIntoEntity() {
        if (entityPlayer == null) {
            if (!loadingProfile || (profileLoader as ThreadPoolExecutor).activeCount == 0) {
                loadingProfile = true
                val playerUUID = UUID.fromString(profile?.uuid?.let { niceUuid(it) })

                // Set needsNewPlayer = false somewhere in here

                profileLoader.submit {
                    val fakeProfile = Minecraft
                        .getMinecraft()
                        .sessionService
                        .fillProfileProperties(GameProfile(playerUUID, null), false)

                    entityPlayer = object : EntityOtherPlayerMP(Minecraft.getMinecraft().theWorld, fakeProfile) {
                        override fun getLocationSkin() : ResourceLocation? {
                            return if (playerLocationSkin == null) DefaultPlayerSkin.getDefaultSkin(this.uniqueID)
                            else playerLocationSkin
                        }

                        override fun getLocationCape() : ResourceLocation? = playerLocationCape

                        override fun getSkinType() : String? {
                            return if (entitySkinType == null) DefaultPlayerSkin.getSkinType(this.uniqueID)
                            else entitySkinType
                        }
                    }

                    entityPlayer?.alwaysRenderNameTag = false
                    entityPlayer?.customNameTag = ""
                }
            }
        } else {
            entityPlayer?.refreshDisplayName()
            val layers: Byte = (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte()
            entityPlayer?.dataWatcher?.updateObject(10, layers)
        }

        if (entityPlayer != null && playerLocationSkin == null) {
            try {
                Minecraft
                    .getMinecraft()
                    .skinManager
                    .loadProfileTextures(
                        entityPlayer?.gameProfile,
                        { type, location1, profileTexture ->
                            when (type) {
                                MinecraftProfileTexture.Type.SKIN -> {
                                    playerLocationSkin = location1
                                    entitySkinType = profileTexture.getMetadata("model")

                                    if (entitySkinType == null) entitySkinType = "default"
                                }
                                MinecraftProfileTexture.Type.CAPE -> {
                                    playerLocationCape = location1
                                }
                            }
                        }, false
                    )
            } catch (ignored: Exception) {}
        }

        populateEntityArmor()
    }

    private fun niceUuid(uuid: String) : String {
        return if (uuid.length != 32) uuid else
                uuid.substring(0, 8) + "-" +
                uuid.substring(8, 12) + "-" +
                uuid.substring(12, 16) + "-" +
                uuid.substring(16, 20) + "-" +
                uuid.substring(20, 32)
    }

    private fun populateEntityArmor() {
        val inventoryInfo = selectedProfile?.inventoryInfo

        if (entityPlayer != null) {
            if (panoramaClickedX != -1 && Mouse.isButtonDown(1)) {
                entityPlayer?.inventory?.armorInventory?.fill(null)
            } else {
                if (inventoryInfo != null && inventoryInfo.containsKey("inv_armor")) {
                    val items = inventoryInfo["inv_armor"]

                    if (items != null && items.size() == 4) {
                        val armorInvSize = entityPlayer?.inventory?.armorInventory?.size ?: 0
                        for (i in 0 until armorInvSize) {
                            val itemElement = items.get(i)

                            if (itemElement != null && itemElement.isJsonObject) {
                                entityPlayer?.inventory?.armorInventory?.set(i,
                                    NotEnoughUpdates.INSTANCE.manager.jsonToStack(itemElement.asJsonObject, false)
                                )
                            }
                        }
                    }
                } else {
                    entityPlayer?.inventory?.armorInventory?.fill(null)
                }
            }

            if (entityPlayer?.uniqueID?.toString() == "ae6193ab-494a-4719-b6e7-d50392c8f012") {
                entityPlayer?.inventory?.armorInventory?.set(3, NotEnoughUpdates.INSTANCE.manager.jsonToStack(
                    NotEnoughUpdates.INSTANCE.manager.itemInformation["SMALL_BACKPACK"]
                )
                )
            }
        }
    }

    private fun loadAndRenderPet() {
        val petsInfo = selectedProfile?.petsInfo

        if (petsInfo != null) {
            val activePetElement = petsInfo.get("active_pet")

            if (activePetElement != null && activePetElement.isJsonObject) {
                val activePet = activePetElement.asJsonObject
                val type = activePet.get("type").asString

                for (i in 0 until 6) {
                    val item = NotEnoughUpdates.INSTANCE.manager.itemInformation["$type;$i"]

                    if (item != null) {
                        val x = position.getAbsX(scaledRes, size[0]) + 2
                        val y = position.getAbsY(scaledRes, size[1]) + 45f + 15f * sin(((currentTime - startTime) / 800f) % (2 * Math.PI))

                        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
                        val stack = NotEnoughUpdates.INSTANCE.manager.jsonToStack(item, false)

                        // Remove extra attributes so no CIT
                        val stackTag = if (stack.tagCompound == null) NBTTagCompound() else stack.tagCompound
                        stackTag.removeTag("ExtraAttributes")
                        stack.tagCompound = stackTag

                        GlStateManager.scale(1.5f, 1.5f, 1f)
                        GlStateManager.enableDepth()
                        Utils.drawItemStack(stack, 0, 0)
                        GlStateManager.scale(1 / 1.5f, 1 / 1.5f, 1f)
                        GlStateManager.translate(-x.toFloat(), -y.toFloat(), 0f)
                        break
                    }
                }
            }
        }
    }

    private fun renderEntity(posX: Int, posY: Int, scale: Int, mouseX: Int, mouseY: Int, ent: EntityLivingBase) {

        // Updates the cape physics when rotating
        ent.onUpdate()
        // Gives the entity model movement in its limbs
        ent.ticksExisted = Minecraft.getMinecraft().thePlayer.ticksExisted

        GlStateManager.enableColorMaterial()
        GlStateManager.pushMatrix()
        GlStateManager.translate(posX.toFloat(), posY.toFloat(), 50f)
        GlStateManager.scale(-scale.toFloat(), scale.toFloat(), scale.toFloat())
        GlStateManager.rotate(180f, 0f, 0f, 1f)
        val renderYawOffset = ent.renderYawOffset
        val f1 = ent.rotationYaw
        val f2 = ent.rotationPitch
        val f3 = ent.prevRotationYawHead
        val f4 = ent.rotationYawHead
        GlStateManager.rotate(135f, 0f, 1f, 0f)
        RenderHelper.enableStandardItemLighting()
        GlStateManager.rotate(-135f, 0f, 1f, 0f)
        GlStateManager.rotate(25f, 1f, 0f, 0f)
        ent.renderYawOffset = atan(mouseX / 40f) * 20f
        ent.rotationYaw = atan(mouseX / 40f) * 40f
        ent.rotationPitch = -atan(mouseY / 40f) * 20f
        ent.rotationYawHead = ent.rotationYaw
        ent.prevRotationYawHead = ent.rotationYaw

        val renderManager = Minecraft.getMinecraft().renderManager
        renderManager.setPlayerViewY(180f)
        renderManager.isRenderShadow = false
        renderManager.renderEntityWithPosYaw(ent, 0.0, 0.0, 0.0, 0f, 1f)

        ent.renderYawOffset = renderYawOffset
        ent.rotationYaw = f1
        ent.rotationPitch = f2
        ent.prevRotationYawHead = f3
        ent.rotationYawHead = f4
        GlStateManager.popMatrix()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        GlStateManager.disableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
    }

    override fun resetCache() {
        entityPlayer = null
        playerLocationSkin = null
        playerLocationCape = null
        entitySkinType = null
        selectedProfile = null
    }

}
