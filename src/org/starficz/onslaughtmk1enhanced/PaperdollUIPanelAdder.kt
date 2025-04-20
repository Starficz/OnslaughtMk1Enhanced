package org.starficz.onslaughtmk1enhanced

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.state.AppDriver
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.combat.CombatState
import org.lazywizard.lazylib.ext.minus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL13.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.util.vector.Vector2f
import org.starficz.onslaughtmk1enhanced.UIFramework.getChildrenCopy
import org.starficz.onslaughtmk1enhanced.UIFramework.*
import org.starficz.onslaughtmk1enhanced.UIFramework.ReflectionUtils.getFieldsMatching
import org.starficz.onslaughtmk1enhanced.UIFramework.ReflectionUtils.invoke
import org.starficz.onslaughtmk1enhanced.UIFramework.centerX
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class PaperdollUIPanelAdder: BaseEveryFrameCombatPlugin() {
    private val noHPColor = Color(200, 30, 30, 255)
    private val fullHPColor = Color(120, 230, 0, 255)

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val state = AppDriver.getInstance().currentState
        if (state !is CombatState) return
        val shipInfo = state.invoke("getShipInfo") as UIPanelAPI

        val uiElements = shipInfo.getChildrenCopy()
        if (uiElements.any { it is CustomPanelAPI && it.plugin is ExtendableCustomUIPanelPlugin }) return // return if added
        val shipField = shipInfo.getFieldsMatching(fieldAssignableTo = ShipAPI::class.java)[0]

        if((shipField.get(shipInfo) as ShipAPI).hullSpec.baseHullId != "onslaught_mk1") return // this plugin would work with all modular ships

        shipInfo.CustomPanel(200f, 200f) { plugin ->
            anchorInBottomLeftOfParent()
            val center = Vector2f(centerX, centerY)

            plugin.render { alpha ->
                initRendering()

                val ship = shipField.get(shipInfo) as ShipAPI
                val targetWidth = ( ship.hullSize.ordinal / 5f ) * 170f
                val moduleScaleFactor = min(targetWidth / max(ship.spriteAPI.width, ship.spriteAPI.height), 2f)

                val shipSprite = ship.spriteAPI
                val shipOffset = Vector2f(shipSprite.centerX - shipSprite.width/2, shipSprite.centerY - shipSprite.height/2).rotate(ship.facing - 90f)
                val shipSpriteLocation = ship.location - shipOffset

                for(module in ship.childModulesCopy){
                    if (module.hitpoints <= 0f) continue

                    val moduleSprite = module.spriteAPI
                    val moduleOffset = Vector2f(moduleSprite.centerX - moduleSprite.width/2, moduleSprite.centerY - moduleSprite.height/2).rotate(module.facing - 90f)
                    val moduleSpriteLocation = module.location - moduleOffset

                    val offset = (shipSpriteLocation - moduleSpriteLocation).scale(moduleScaleFactor) as Vector2f
                    val paperDollLocation = center - offset

                    val armorHealthLevel = with(module.armorGrid) {
                        (armorAtCell(weakestArmorRegion()!!)!! + module.hitpoints) / (armorRating + module.maxHitpoints)
                    }

                    val sprite = Global.getSettings().getSprite(module.hullSpec.spriteName).apply {
                        angle = module.facing - 90f
                        color = interpolateColorNicely(noHPColor, fullHPColor, armorHealthLevel)
                        alphaMult = 0.75f * alpha
                        setSize(width * moduleScaleFactor, height * moduleScaleFactor)
                    }

                    sprite.renderAtCenter(paperDollLocation.x, paperDollLocation.y)
                }

                glPopAttrib()
            }
        }
    }

    private fun initRendering(){
        // Save GL state (includes texture, blend, matrix modes, texenv, etc.)
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        // Use GL_COMBINE to allow arbitrary uniform colors
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE)

        // RGB Combine -> Use Primary Color (from glColor, set internally by spriteAPI.render based on spriteAPI.color)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_REPLACE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_RGB, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR)

        // Alpha Combine -> Modulate Texture Alpha * Primary Color Alpha (from glColor)
        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC0_ALPHA, GL_TEXTURE)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_ALPHA, GL_SRC_ALPHA)
        glTexEnvi(GL_TEXTURE_ENV, GL_SRC1_ALPHA, GL_PRIMARY_COLOR)
        glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_ALPHA, GL_SRC_ALPHA)
    }
}