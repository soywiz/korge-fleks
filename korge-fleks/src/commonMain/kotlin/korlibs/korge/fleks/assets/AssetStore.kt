package korlibs.korge.fleks.assets

import entities.config.common.*
import korlibs.audio.format.*
import korlibs.audio.sound.*
import korlibs.datastructure.*
import korlibs.image.atlas.MutableAtlasUnit
import korlibs.image.bitmap.*
import korlibs.image.font.Font
import korlibs.image.font.readBitmapFont
import korlibs.image.format.*
import korlibs.image.tiles.tiled.*
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.ldtk.*
import korlibs.korge.ldtk.view.*
import korlibs.time.Stopwatch
import kotlin.collections.set
import kotlin.reflect.*


/**
 * This class is responsible to load all kind of game data and make it usable / consumable by entities of Korge-Fleks.
 *
 * Assets are separated into [Common][AssetType.COMMON], [World][AssetType.WORLD], [Level][AssetType.LEVEL] and
 * [Special][AssetType.SPECIAL] types. The 'Common' type means that the asset is used throughout
 * the game. So it makes sense to not reload those assets on every level or world. The same applies also for 'World' type.
 * It means a world-asset is used in all levels of a world. An asset of type 'Level' means that it is really only used in
 * one level (e.g. level specific graphics or music). The 'Special' type of assets is meant to be used for loading assets
 * during a level which should be unloaded also within the level. This can be used for extensive graphics for a mid-level
 * boss. After the boss has been beaten the graphics can be unloaded since they are not needed anymore.
 */
class AssetStore {
    val commonAtlas: MutableAtlasUnit = MutableAtlasUnit(1024, 2048, border = 1)
    val worldAtlas: MutableAtlasUnit = MutableAtlasUnit(1024, 2048, border = 1)
    val levelAtlas: MutableAtlasUnit = MutableAtlasUnit(1024, 2048, border = 1)
    val specialAtlas: MutableAtlasUnit = MutableAtlasUnit(1024, 2048, border = 1)

//    @Volatile
    internal var commonAssetConfig: AssetModel = AssetModel()
    internal var currentWorldAssetConfig: AssetModel = AssetModel()
    internal var currentLevelAssetConfig: AssetModel = AssetModel()
    internal var specialAssetConfig: AssetModel = AssetModel()

    internal var tiledMaps: MutableMap<String, Pair<AssetType, TiledMap>> = mutableMapOf()
    internal var ldtkWorld: MutableMap<String, Pair<AssetType, LDTKWorld>> = mutableMapOf()
    internal var backgrounds: MutableMap<String, Pair<AssetType, ParallaxDataContainer>> = mutableMapOf()
    internal var images: MutableMap<String, Pair<AssetType, ImageDataContainer>> = mutableMapOf()
    internal var fonts: MutableMap<String, Pair<AssetType, Font>> = mutableMapOf()
    internal var sounds: MutableMap<String, Pair<AssetType, SoundChannel>> = mutableMapOf()

    fun getSound(name: String) : SoundChannel =
        if (sounds.contains(name)) sounds[name]!!.second
        else error("AssetStore: Sound '$name' not found!")

    fun getImageData(name: String, slice: String? = null) : ImageData =
        if (images.contains(name)) {
            if (slice == null) {
                images[name]!!.second.default
            } else {
                if (images[name]!!.second[slice] != null) {
                    images[name]!!.second[slice]!!
                } else {
                    println("WARNING - AssetStore: Slice '$slice' of image '$name' not found!")
                    ImageData()
                }
            }
        } else {
            println("WARNING - AssetStore: Image '$name' not found!")
            ImageData()
        }

    fun getLdtkWorld(name: String) : LDTKWorld =
        if (ldtkWorld.contains(name)) ldtkWorld[name]!!.second
        else error("AssetStore: LDtkWorld '$name' not found!")

    fun getLdtkLevel(ldtkWorld: LDTKWorld, levelName: String) : Level =
        if (ldtkWorld.levelsByName.contains(levelName)) ldtkWorld.levelsByName[levelName]!!.level
        else error("AssetStore: LDtkLevel '$levelName' not found!")

    fun loadEntitiesForLdtkLevel(worldName: String, levelName: String) {
        // Create all game objects and save their entity config in the EntityFactory
        val entityLayer = getLdtkLevel(getLdtkWorld(worldName), levelName).layerInstances?.firstOrNull { it.entityInstances.isNotEmpty() }
        entityLayer?.entityInstances?.forEach {
            println("INFO: Entity config loaded with ID '${it.defUid}' (${it.identifier})")
            GameObjectConfig(
                name = "uid_${it.defUid}",
                ldtkIdentifier = it.identifier,
                x = it.px[0].toFloat(),
                y = it.px[1].toFloat()
            )
        }
    }

    fun getNinePatch(name: String) : NinePatchBmpSlice =
        if (images.contains(name)) {
            val layerData = images[name]!!.second.imageDatas.first().frames.first().first
            if (layerData != null) {
                val ninePatch = layerData.ninePatchSlice
                ninePatch ?: error("AssetStore: Image '$name' does not contain nine-patch data!")
            } else error("AssetStore: Image layer of '$name' not found!")
        } else error("AssetStore: Image '$name' not found!")

    fun getBackground(name: String) : ParallaxDataContainer =
        if (backgrounds.contains(name)) backgrounds[name]!!.second
        else error("AssetStore: Parallax background '$name' not found!")

    fun getTiledMap(name: String) : TiledMap {
        return if (tiledMaps.contains(name)) tiledMaps[name]!!.second
        else error("AssetStore: TiledMap '$name' not found!")
    }

    fun getFont(name: String) : Font {
        return if (fonts.contains(name)) fonts[name]!!.second
        else error("AssetStore: Cannot find font '$name'!")
    }

    suspend fun loadAssets(type: AssetType, assetConfig: AssetModel) {
        var assetLoaded = false
        val atlas = when (type) {
            AssetType.COMMON -> {
                prepareCurrentAssets(type, assetConfig, commonAssetConfig)?.also { config ->
                    commonAssetConfig = config
                    assetLoaded = true
                }
                commonAtlas
            }
            AssetType.WORLD -> {
                prepareCurrentAssets(type, assetConfig, currentWorldAssetConfig)?.also { config ->
                    currentWorldAssetConfig = config
                    assetLoaded = true
                }
                worldAtlas
            }
            AssetType.LEVEL -> {
                prepareCurrentAssets(type, assetConfig, currentLevelAssetConfig)?.also { config ->
                    currentLevelAssetConfig = config
                    assetLoaded = true
                }
                levelAtlas
            }
            AssetType.SPECIAL -> {
                prepareCurrentAssets(type, assetConfig, specialAssetConfig)?.also { config ->
                    specialAssetConfig = config
                    assetLoaded = true
                }
                specialAtlas
            }
        }

        if (assetLoaded) {

            val sw = Stopwatch().start()
            println("AssetStore: Start loading [${type.name}] resources from '${assetConfig.folder}'...")

            // Update maps of music, images, ...
            assetConfig.tileMaps.forEach { tileMap ->
                when (tileMap.value.type) {
                    TileMapType.LDTK -> ldtkWorld[tileMap.key] = Pair(type, resourcesVfs[assetConfig.folder + "/" + tileMap.value.fileName].readLDTKWorld(extrude = true))
                    TileMapType.TILED -> tiledMaps[tileMap.key] = Pair(type, resourcesVfs[assetConfig.folder + "/" + tileMap.value.fileName].readTiledMap(atlas = atlas))
                }
            }

            assetConfig.sounds.forEach { sound ->
                val soundFile = resourcesVfs[assetConfig.folder + "/" + sound.value].readMusic()
                val soundChannel = soundFile.decode().toWav().readMusic().play()  // -- convert to WAV
//                val soundChannel = soundFile.play()
//            val soundChannel = resourcesVfs[assetConfig.assetFolderName + "/" + sound.value].readSound().play()

                soundChannel.pause()
                sounds[sound.key] = Pair(type, soundChannel)
            }
            assetConfig.backgrounds.forEach { background ->
                backgrounds[background.key] = Pair(type, resourcesVfs[assetConfig.folder + "/" + background.value.aseName].readParallaxDataContainer(background.value, ASE, atlas = atlas))
            }
            assetConfig.images.forEach { image ->
                images[image.key] = Pair(
                    type,
                    if (image.value.layers == null) {
                        resourcesVfs[assetConfig.folder + "/" + image.value.fileName].readImageDataContainer(ASE.toProps(), atlas = atlas)
                    } else {
                        val props = ASE.toProps() // TODO check -- ImageDecodingProps(it.value.fileName, extra = ExtraTypeCreate())
                        props.setExtra("layers", image.value.layers)
                        resourcesVfs[assetConfig.folder + "/" + image.value.fileName].readImageDataContainer(props, atlas)
                    }
                )
            }
            assetConfig.fonts.forEach { font ->
                fonts[font.key] = Pair(type, resourcesVfs[assetConfig.folder + "/" + font.value].readBitmapFont(atlas = atlas))
            }

            println("Assets: Loaded resources in ${sw.elapsed}")

            if (assetConfig.hotReloading) {
                configureResourceDirWatcher {
                    addAssetWatcher(type) {}
                }
            }
        }
    }

    private fun prepareCurrentAssets(type: AssetType, newAssetConfig: AssetModel, currentAssetConfig: AssetModel): AssetModel? =
        when (currentAssetConfig.folder) {
            "" -> {
                // Just load new assets
                newAssetConfig
            }
            newAssetConfig.folder -> {
                if (newAssetConfig.hotReloading) {
//                    removeAssets(newAssetConfig.type)
                    println("INFO: Reload $type assets '${newAssetConfig.folder}'.")
                    newAssetConfig
                } else {
                    println("INFO: $type assets '${newAssetConfig.folder}' already loaded! No reload is happening!")
                    null
                }
            }
            else -> {
                println("INFO: Remove old $type assets and load new ones!")
                removeAssets(type)
                newAssetConfig
            }
        }

    /**
     * Remove all assets which have a specific given [AssetType].
     */
    private fun removeAssets(type: AssetType) {
        tiledMaps.entries.removeAll { it.value.first == type }
        backgrounds.entries.removeAll { it.value.first == type }
        images.entries.removeAll { it.value.first == type }
        fonts.entries.removeAll { it.value.first == type }
        sounds.entries.removeAll { it.value.first == type }
    }
}
