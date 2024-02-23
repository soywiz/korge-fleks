package korlibs.korge.fleks.entity.config

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import korlibs.korge.assetmanager.AssetStore
import korlibs.korge.assetmanager.ConfigBase
import korlibs.korge.fleks.components.*
import korlibs.korge.fleks.utils.*


object TextAndLogos {

    data class LogoConfig(
        val centerX: Boolean = false,
        val centerY: Boolean = false,
        val offsetX: Float = 0.0f,
        val offsetY: Float = 0.0f,

        val logoName: String? = null,
        val text: String? = null,
        val fontName: String = "",

        val alpha: Float = 0.0f,
        val drawOnLayer: String? = null,

        val function: Identifier? = null,
        val config: Identifier = nothing
    ) : ConfigBase

    data class LogoLayerConfig(
        var layerName: String = "",
        var offsetX: Double = 0.0,
        var offsetY: Double = 0.0,
        val alpha: Float = 0f,
        var parentEntity: Entity = invalidEntity
    ) : ConfigBase

    // Used in component properties to specify invokable function
    val configureLogo = Identifier(name = "configureLogo")
    val configureLogoLayer = Identifier(name = "configureLogoLayer")

    private val configureLogoFct = fun(world: World, entity: Entity, config: Identifier) = with(world) {
        val logoConfig = AssetStore.getEntityConfig<LogoConfig>(config.name)
        entity.configure { entity ->
            // Make sure we have position component
            entity.getOrAdd(PositionShapeComponent) { PositionShapeComponent() }

            logoConfig.logoName?.let {
                entity.getOrAdd(SpriteComponent) { SpriteComponent() }.also {
                    it.assetName = logoConfig.logoName
                }
            }
            logoConfig.text?.let {
                entity.getOrAdd(TextComponent) { TextComponent() }.also {
                    it.text = logoConfig.text
                    it.fontName = logoConfig.fontName
                }
            }

            entity += PositionShapeComponent()
            entity.getOrAdd(LayoutComponent) { LayoutComponent() }.also {
                it.centerX = logoConfig.centerX
                it.centerY = logoConfig.centerY
                it.offsetX = logoConfig.offsetX
                it.offsetY = logoConfig.offsetY
            }
            logoConfig.drawOnLayer?.let {
                entity.getOrAdd(DrawableComponent) { DrawableComponent() }.also {
                    it.drawOnLayer = logoConfig.drawOnLayer
                }
            }
            entity.getOrAdd(AppearanceComponent) { AppearanceComponent() }.also {
                it.alpha = logoConfig.alpha
            }
            entity += LifeCycleComponent()
            logoConfig.function?.let { invokable ->
                entity.getOrAdd(InputTouchButtonComponent) { InputTouchButtonComponent() }.also {
                    it.function = invokable
                    it.config = logoConfig.config
                }
            }
        }
        entity
    }

    private val configureLogoLayerFct = fun(world: World, entity: Entity, config: Identifier) = with(world) {
        val layerConfig = AssetStore.getEntityConfig<LogoLayerConfig>(config.name)
        entity.configure { entity ->
            entity.getOrAdd(SpecificLayerComponent) { SpecificLayerComponent() }.also {
                it.spriteLayer = layerConfig.layerName
                it.parentEntity = layerConfig.parentEntity
            }
            entity.getOrAdd(PositionShapeComponent) { PositionShapeComponent() }  // x, y will be set to view in SpecificLayer hook function
            entity.getOrAdd(OffsetComponent) { OffsetComponent() }.also {
                it.x = layerConfig.offsetX
                it.y = layerConfig.offsetY
            }
            entity.getOrAdd(AppearanceComponent) { AppearanceComponent() }.also {
                it.alpha = layerConfig.alpha
            }
        }
        entity
    }

    init {
        Invokable.register(configureLogo, configureLogoFct)
        Invokable.register(configureLogoLayer, configureLogoLayerFct)
    }
}
