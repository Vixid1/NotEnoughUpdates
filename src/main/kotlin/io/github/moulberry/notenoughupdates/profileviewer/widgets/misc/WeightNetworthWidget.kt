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

import io.github.moulberry.notenoughupdates.NotEnoughUpdates
import io.github.moulberry.notenoughupdates.core.config.Position
import io.github.moulberry.notenoughupdates.core.util.StringUtils
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewer
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.getSelectedProfile
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.profile
import io.github.moulberry.notenoughupdates.profileviewer.ProfileViewerScreen.Companion.profileName
import io.github.moulberry.notenoughupdates.profileviewer.SkyblockProfiles
import io.github.moulberry.notenoughupdates.profileviewer.weight.lily.LilyWeight
import io.github.moulberry.notenoughupdates.profileviewer.weight.senither.SenitherWeight
import io.github.moulberry.notenoughupdates.profileviewer.widgets.WidgetInterface
import io.github.moulberry.notenoughupdates.util.Constants
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import org.apache.commons.lang3.text.WordUtils
import org.lwjgl.input.Keyboard
import kotlin.math.round

class WeightNetworthWidget(
    override val widgetName: String,
    override var position: Position,
    override val shadowText: Boolean,
    override var size: MutableList<Int> = mutableListOf(120, 34)
) : WidgetInterface {

    // TODO: Find out why weight and nw dont reset between different players

    private lateinit var scaledRes: ScaledResolution

    private var selectedProfile: SkyblockProfiles.SkyblockProfile? = getSelectedProfile()

    private var posX: Int = 0
    private var posY: Int = 0

    override fun render(mouseX: Int, mouseY: Int) {
        scaledRes = ScaledResolution(Minecraft.getMinecraft())
        if (selectedProfile == null) selectedProfile = getSelectedProfile()

        posX = position.getAbsX(scaledRes, size[0])
        posY = position.getAbsY(scaledRes, size[1])

        if (NotEnoughUpdates.INSTANCE.config.profileViewer.displayWeight) renderWeight(mouseX, mouseY)
        renderNetworth(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
    }

    override fun resetCache() {}

    private fun renderWeight(mouseX: Int, mouseY: Int) {
        if (selectedProfile?.skillsApiEnabled() == false) return

        val skyblockInfo: MutableMap<String, ProfileViewer.Level> = selectedProfile?.levelingInfo ?: return

        if (Constants.WEIGHT == null || Utils.getElement(Constants.WEIGHT, "lily.skills.overall") == null
            || !Utils.getElement(Constants.WEIGHT, "lily.skills.overall").isJsonPrimitive) {
            Utils.showOutdatedRepoNotification()
            return
        }

        val fr = Minecraft.getMinecraft().fontRendererObj
        val senitherWeight = SenitherWeight(skyblockInfo)
        val lilyWeight = LilyWeight(skyblockInfo, selectedProfile?.profileJson)

        var weight: Long? = -2L
        if (NotEnoughUpdates.INSTANCE.config.profileViewer.useSoopyNetworth) weight = profile?.soopyWeightLeaderboardPosition

        // Senither Weight
        Utils.drawStringCentered("§aSenither Weight: §6" +
                StringUtils.formatNumber(round(senitherWeight.totalWeight.raw).toInt()),
            posX + size[0] / 2f, posY + 8f, true, 0)

        val senitherWidth = fr.getStringWidth("Senither Weight" +
                StringUtils.formatNumber(round(senitherWeight.totalWeight.raw).toInt()))

        if (mouseX > posX + (size[0] / 2) - senitherWidth / 2 && mouseX < posX + (size[0] / 2) + senitherWidth / 2 &&
            mouseY > posY + 8 - fr.FONT_HEIGHT / 2 && mouseY < posY + 8 + fr.FONT_HEIGHT / 2) {

            val tooltipToDisplay: ArrayList<String> = arrayListOf()
            tooltipToDisplay.add("§aSkills: §6" +
                    StringUtils.formatNumber(round(senitherWeight.skillsWeight.weightStruct.raw).toInt()))
            tooltipToDisplay.add("§aSlayer: §6" +
                    StringUtils.formatNumber(round(senitherWeight.slayerWeight.weightStruct.raw).toInt()))
            tooltipToDisplay.add("§aDungeons: §6" +
                    StringUtils.formatNumber(round(senitherWeight.dungeonsWeight.weightStruct.raw).toInt()))

            if (NotEnoughUpdates.INSTANCE.config.profileViewer.useSoopyNetworth &&
                profile?.isProfileMaxSoopyWeight(profileName) == true) {

                val lbPos = "§2#§6" + StringUtils.formatNumber(profile?.soopyWeightLeaderboardPosition)
                tooltipToDisplay.add("")

                var state = "§cAn error occurred"
                if (weight == -2L) state = "§eLoading"
                if (weight != null) {
                    if (weight > 0) {
                        tooltipToDisplay.add("$lbPos§a on soopy's weight leaderboard!")
                    } else {
                        tooltipToDisplay.add("$state§a on soopy's weight leaderboard!")
                    }
                }
            }

            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,
                Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, -1)
        }

        // Lily Weight
        Utils.drawStringCentered("§aLily Weight: §6" +
                StringUtils.formatNumber(round(lilyWeight.totalWeight.raw).toInt()),
            posX + size[0] / 2f, posY + 18f, true, 0)

        val lilyWidth = fr.getStringWidth("Lily Weight: " +
                StringUtils.formatNumber(round(lilyWeight.totalWeight.raw).toInt()))

        if (mouseX > posX + (size[0] / 2) - lilyWidth / 2 && mouseX < posX + (size[0] / 2) + lilyWidth / 2 &&
            mouseY > posY + 18 - fr.FONT_HEIGHT / 2 && mouseY < posY + 18 + fr.FONT_HEIGHT / 2) {

            val tooltipToDisplay: ArrayList<String> = arrayListOf()
            tooltipToDisplay.add("§aSkills: §6" +
                    StringUtils.formatNumber(round(lilyWeight.skillsWeight.weightStruct.raw).toInt()))
            tooltipToDisplay.add("§aSlayer: §6" +
                    StringUtils.formatNumber(round(lilyWeight.slayerWeight.weightStruct.raw).toInt()))
            tooltipToDisplay.add("§aDungeons: §6" +
                    StringUtils.formatNumber(round(lilyWeight.dungeonsWeight.weightStruct.raw).toInt()))

            Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,
                Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, -1)
        }
    }

    private fun renderNetworth(mouseX: Int, mouseY: Int) {
        val fr = Minecraft.getMinecraft().fontRendererObj

        var state = "§aAn error occurred"

        var networth: Long = -2
        val nwCategoryHover: ArrayList<String> = arrayListOf()

        if (NotEnoughUpdates.INSTANCE.config.profileViewer.useSoopyNetworth) {
            val nwData = selectedProfile?.getSoopyNetworth {}

            if (nwData != null) {
                networth = nwData.networth
            }

            if (networth == -1L) {
                state = "§eLoading..."
            } else if (networth != -2L) { // -2 indicates error
                val categoryTotalEntries = nwData?.categoryToTotal?.entries ?: return

                for (entry in categoryTotalEntries) {
                    nwCategoryHover.add(
                        "§a" + WordUtils.capitalizeFully(entry.key.replace("_", " ")) +
                                ": §6" + StringUtils.formatNumber(entry.value)
                    )
                }

                nwCategoryHover.add("")
            }
        }

        // Calculate using NEU networth if not using soopy networth or soopy nw errored
        if (networth == -2L) {
            networth = selectedProfile?.networth ?: -1
        }

        if (networth > 0) {
            Utils.drawStringCentered("§aNet Worth: §6" + StringUtils.formatNumber(networth),
                posX + size[0] / 2f, posY + 28f, false, 0)

            val nwWidth = fr.getStringWidth("Net Worth: " + StringUtils.formatNumber(networth))

            if (NotEnoughUpdates.INSTANCE.manager.auctionManager.getBazaarInfo("BOOSTER_COOKIE") != null &&
                NotEnoughUpdates.INSTANCE.manager.auctionManager.getBazaarInfo("BOOSTER_COOKIE").has("avg_buy")) {
                val nwInCookies = (networth /
                        NotEnoughUpdates.INSTANCE.manager.auctionManager
                            .getBazaarInfo("BOOSTER_COOKIE")
                            .get("avg_buy")
                            .asDouble)

                val networthInIRL = StringUtils.formatNumber(round(((nwInCookies * 325) / 675) * 4.99))

                if (mouseX > posX + (size[0] / 2) - nwWidth / 2 && mouseX < posX + (size[0] / 2) + nwWidth / 2 &&
                    mouseY > posY + 28 - fr.FONT_HEIGHT / 2 && mouseY < posY + 28 + fr.FONT_HEIGHT / 2) {

                    val tooltipToDisplay: ArrayList<String> = arrayListOf()
                    tooltipToDisplay.add("§aNet worth in IRL money: §2$§6$networthInIRL")

                    if (NotEnoughUpdates.INSTANCE.config.profileViewer.useSoopyNetworth &&
                        profile?.soopyNetworthLeaderboardPosition!! >= 0 &&
                        profile?.isProfileMaxSoopyWeight(profileName) == true) {

                        val lbPos = "§2#§6" + StringUtils.formatNumber(profile?.soopyNetworthLeaderboardPosition)
                        tooltipToDisplay.add("")
                        tooltipToDisplay.add("$lbPos §aon soopy's networth leaderboard!")
                    }

                    if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                        tooltipToDisplay.addAll(nwCategoryHover)
                        tooltipToDisplay.add("§cThis is calculated using the current")
                        tooltipToDisplay.add("§cprice of booster cookies on bazaar and the price")
                        tooltipToDisplay.add("§cfor cookies using gems, then the price of gems")
                        tooltipToDisplay.add("§cis where we get the amount of IRL money you")
                        tooltipToDisplay.add("§ctheoretically have on SkyBlock in net worth.")
                    } else {
                        tooltipToDisplay.add("§7[SHIFT for Info]")
                    }
                    if (!NotEnoughUpdates.INSTANCE.config.hidden.dev) {
                        tooltipToDisplay.add("")
                        tooltipToDisplay.add("§cTHIS IS IN NO WAY ENDORSING IRL TRADING!")
                    }

                    Utils.drawHoveringText(tooltipToDisplay, mouseX, mouseY,
                        Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight, -1)
                }
            }
        } else {
            Utils.drawStringCentered("§aNet Worth: $state", posX + size[0] / 2f, posY + 28f, true, 0)
        }
    }
}
