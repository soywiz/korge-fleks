package korlibs.korge.fleks.components

import com.github.quillraven.fleks.*
import korlibs.korge.fleks.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable @SerialName("NinePatchComponent")
data class NinePatchComponent(
    val name: String = "",
    val width: Float = 0f,
    val height: Float = 0f
) : CloneableComponent<NinePatchComponent>() {
    override fun type() = NinePatchComponent

    companion object : ComponentType<NinePatchComponent>()

    // Author's hint: Check if deep copy is needed on any change in the component!
    override fun clone(): NinePatchComponent =
        this.copy()
}
