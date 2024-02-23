package korlibs.korge.fleks.systems

import com.github.quillraven.fleks.*
import com.github.quillraven.fleks.World.Companion.family
import korlibs.image.color.RGBA
import korlibs.korge.fleks.components.*
import korlibs.korge.fleks.components.SpriteComponent
import korlibs.korge.fleks.utils.KorgeViewCache
import korlibs.korge.fleks.utils.random
import korlibs.korge.parallax.ImageDataViewEx
import korlibs.korge.parallax.ParallaxDataView
import korlibs.time.TimeSpan

/**
 * This system is updating the view objects for all [DrawableComponent] entities.
 *
 * In case the [DrawableComponent] entity is of type [SpriteComponent] it takes the image configuration from
 * [SpriteComponent] component to setup and control the sprite animations.
 */
class KorgeViewSystem : IteratingSystem(
    family { all(AppearanceComponent).any(AppearanceComponent, SwitchLayerVisibilityComponent, SpecificLayerComponent, PositionShapeComponent, OffsetComponent) },
    interval = EachFrame
) {
    var updateViewsEnabled: Boolean = true
    private var lastY: Double = 0.0

    override fun onTickEntity(entity: Entity) {
        val appearance = entity[AppearanceComponent]

        if (updateViewsEnabled) {
            // TODO this can be re-written with help of SpecificLayer ???
            entity.getOrNull(SwitchLayerVisibilityComponent)?.let { visibility ->
                visibility.spriteLayers.forEach { layer ->
                    layer.visible = if (layer.visible) {
                        // try to switch off
                        (0.0..1.0).random() > visibility.offVariance  // variance in switching value off - 1: every frame switching possible - 0: no switching at all
                    } else {
                        // try to switch on again
                        (0.0..1.0).random() <= visibility.onVariance  // variance in switching value on again - 1: changed value switches back immediately - 0: changed value stays forever
                    }
                    (KorgeViewCache[entity] as ImageDataViewEx).getLayer(layer.name)?.visible = layer.visible
                }
            }
        }

        val offset: Point = if (entity hasNo OffsetComponent) Point()
        else Point(entity[OffsetComponent].x, entity[OffsetComponent].y)
        entity.getOrNull(OffsetByFrameIndexComponent)?.let {
            // Get offset depending on current animation and frame index
            val currentFrameIndex = (KorgeViewCache[it.entity] as ImageDataViewEx).currentFrameIndex
            val animationName = it.entity.getOrNull(SpriteComponent)?.animationName ?: ""
            val frameOffset = it.list[animationName]?.get(currentFrameIndex)
                ?: error("KorgeViewSystem: Cannot get offset by frame index (entity: ${entity.id}, animationName: '$animationName', currentFrameIndex: $currentFrameIndex)")
            offset.x += frameOffset.x
            offset.y += frameOffset.y
        }

        KorgeViewCache[entity].let { view ->
            if (appearance.visible) {
                view.visible = true
                view.alpha = appearance.alpha.toDouble()
                appearance.tint?.also { tint -> view.colorMul = RGBA(tint.r, tint.g, tint.b, 0xff) }

                if (entity has PositionShapeComponent) {
                    val positionShapeComponent = entity[PositionShapeComponent]
                    view.x = (positionShapeComponent.x - offset.x).toDouble()
                    view.y = (positionShapeComponent.y - offset.y).toDouble()
                }

//                println("[${entity.id}] Y: ${view.y} (Position: ${positionShape.y} delta: ${lastY - positionShape.y})")
//                lastY = positionShape.y

            } else {
                view.visible = false
            }

            if (updateViewsEnabled) {
                if (view is ImageDataViewEx) view.update(TimeSpan(deltaTime.toDouble() * 1000.0))
                else if (view is ParallaxDataView) view.update(TimeSpan(deltaTime.toDouble() * 1000.0))
            }

        }
    }
}
