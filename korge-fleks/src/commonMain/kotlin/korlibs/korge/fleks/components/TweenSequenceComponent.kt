package korlibs.korge.fleks.components

import com.github.quillraven.fleks.*
import korlibs.image.format.*
import korlibs.korge.fleks.utils.*
import korlibs.korge.fleks.components.RgbaComponent.Rgb
import korlibs.math.interpolation.Easing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * This component holds all needed details to animate properties of components of entities.
 */
@Serializable @SerialName("TweenSequence")
data class TweenSequenceComponent(
    var tweens: List<TweenBase> = listOf(),

    // Internal runtime data
    var index: Int = 0,              // This points to the animation step which is currently in progress
    var timeProgress: Float = 0f,    // Elapsed time for the object to be animated
    var waitTime: Float = 0f,
    var executed: Boolean = false,
    var initialized: Boolean = false
) : Component<TweenSequenceComponent> {
    override fun type(): ComponentType<TweenSequenceComponent> = TweenSequenceComponent

    /**
     * Initialize internal waitTime property with delay value of first tweens if available.
     */
    override fun World.onAdd(entity: Entity) {
        // Make sure that initialization is skipped on world snapshot loading (deserialization of save game)
        if (initialized) return
        else initialized = true

        if (tweens.isNotEmpty()) waitTime = tweens[index].delay ?: 0f
    }

    interface TweenBase {
        var entity: Entity
        var delay: Float?
        var duration: Float?
        var easing: Easing?
    }

    /**
     * Animation Component data classes based on TweenBase
     */
    @Serializable
    @SerialName("SpawnNewTweenSequence")
    data class SpawnNewTweenSequence(
        val tweens: List<TweenBase> = listOf(),       // tween objects which contain entity and its properties to be animated in sequence

        override var entity: Entity = Entity.NONE,    // not used
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null           // not used
    ) : TweenBase

    @Serializable
    @SerialName("ParallelTweens")
    data class ParallelTweens(
        val tweens: List<TweenBase> = listOf(),       // tween objects which contain entity and its properties to be animated in parallel

        override var entity: Entity = Entity.NONE,    // not used here
        override var delay: Float? = 0f,              // in seconds
        override var duration: Float? = 0f,           // in seconds
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = Easing.LINEAR  // function to change the properties
    ) : TweenBase

    @Serializable
    @SerialName("Wait")
    data class Wait(
        override var entity: Entity = Entity.NONE,    // not used
        override var delay: Float? = null,            // Not used
        override var duration: Float? = 0f,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null           // not used
    ) : TweenBase

    @Serializable
    @SerialName("DeleteEntity")
    data class DeleteEntity(
        val healthCounter: Int = 0,             // set healthCounter to zero to delete the entity immediately

        override var entity: Entity,
        override var delay: Float? = null,      // not used
        override var duration: Float? = null,   // not used
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null     // not used
    ) : TweenBase

    @Serializable
    @SerialName("SpawnEntity")
    data class SpawnEntity(
        var entityConfig: String,           // name of the entity configuration which creates and configures the spawned entity
        var x: Float = 0f,                  // position where entity will be spawned
        var y: Float = 0f,

        override var entity: Entity = Entity.NONE, // when entity is not given (=Entity.NONE) than it will be created
        override var delay: Float? = null,
        override var duration: Float? = 0f,    // not used - 0f for immediately
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null    // not used
    ) : TweenBase

    @Serializable
    @SerialName("ExecuteConfigFunction")
    data class ExecuteConfigFunction(
        var entityConfig: String,                     // name of entity config which contains the function to configure the spawned entity

        override var entity: Entity = Entity.NONE,    // [optional] entity can be provided if needed in the configure-function
        override var delay: Float? = null,            // not used
        override var duration: Float? = null,         // not used
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null           // not used
    ) : TweenBase


    /**
     *  Following component classes are for triggering tweens on specific properties of components
     */
    @Serializable
    @SerialName("TweenRgba")
    data class TweenRgba(
        val alpha: Float? = null,
        val tint: Rgb? = null,
        val visible: Boolean? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenPosition")
    data class TweenPosition(
        val x: Float? = null,
        val y: Float? = null,
        val offsetX: Float? = null,
        val offsetY: Float? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenLayout")
    data class TweenLayout(
        val centerX: Boolean? = null,
        val centerY: Boolean? = null,
        val offsetX: Float? = null,
        val offsetY: Float? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenSprite")
    data class TweenSprite(
        var animation: String? = null,
        // do not tween the frameIndex, it is updated by the SpriteSystem
        var running: Boolean? = null,
        var direction: ImageAnimation.Direction? = null,
        var destroyOnPlayingFinished: Boolean? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenSwitchLayerVisibility")
    data class TweenSwitchLayerVisibility(
        var offVariance: Float? = null,
        var onVariance: Float? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenSpawner")
    data class TweenSpawner(
        var numberOfObjects: Int? = null,
        var interval: Int? = null,
        var timeVariation: Int? = null,
        var positionVariation: Float? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    @Serializable
    @SerialName("TweenSound")
    data class TweenSound(
        var startTrigger: Boolean? = null,
        var stopTrigger: Boolean? = null,
        var position: Float? = null,
        var volume: Float? = null,

        override var entity: Entity,
        override var delay: Float? = null,
        override var duration: Float? = null,
        @Serializable(with = EasingSerializer::class)
        override var easing: Easing? = null
    ) : TweenBase

    companion object : ComponentType<TweenSequenceComponent>()
}

