package com.boydti.fawe

import com.boydti.fawe.`object`.collection.BitArray4096
import com.boydti.fawe.`object`.collection.CleanableThreadLocal
import com.boydti.fawe.`object`.collection.VariableThreadLocal
import com.boydti.fawe.`object`.exception.FaweBlockBagException
import com.boydti.fawe.`object`.exception.FaweChunkLoadException
import com.boydti.fawe.`object`.exception.FaweException
import com.boydti.fawe.beta.Trimable
import com.boydti.fawe.beta.implementation.queue.Pool
import com.boydti.fawe.beta.implementation.queue.QueuePool
import com.boydti.fawe.config.Settings
import com.boydti.fawe.util.IOUtil
import com.boydti.fawe.util.MathMan
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.sk89q.jnbt.*
import com.sk89q.worldedit.math.MutableBlockVector3
import com.sk89q.worldedit.math.MutableVector3
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent
import com.sk89q.worldedit.world.block.BlockTypesCache
import org.slf4j.LoggerFactory.getLogger
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import java.util.function.Supplier

object FaweCache : Trimable {

    const val BLOCKS_PER_LAYER = 4096
    const val chunkLayers = 16
    const val worldHeight = chunkLayers shl 4
    const val worldMaxY = 255

    val EMPTY_CHAR_4096 = CharArray(4096)

    private val REGISTERED_SINGLETONS = IdentityHashMap<Class<*>, CleanableThreadLocal<*>>()
    private val REGISTERED_POOLS = IdentityHashMap<Class<*>, Pool<*>>()

    /*
    thread cache
     */
    val CHUNK_FLAG = CleanableThreadLocal(Supplier { AtomicBoolean() }) // resets to false

    val LONG_BUFFER_1024 = CleanableThreadLocal { LongArray(1024) }

    val BYTE_BUFFER_8192 = CleanableThreadLocal { ByteArray(8192) }

    val BYTE_BUFFER_VAR = VariableThreadLocal()

    val BLOCK_TO_PALETTE = CleanableThreadLocal {
        val result = IntArray(BlockTypesCache.states.size)
        Arrays.fill(result, Integer.MAX_VALUE)
        result
    }

    val sectionBitsToChar = CleanableThreadLocal { CharArray(4096) }

    val PALETTE_TO_BLOCK = CleanableThreadLocal { IntArray(Character.MAX_VALUE.toInt() + 1) }

    val PALETTE_TO_BLOCK_CHAR = CleanableThreadLocal<CharArray>(
            fun(): CharArray {
                return CharArray(Char.MAX_VALUE.toInt() + 1)
            }, fun(a: CharArray): Unit = Arrays.fill(a, Char.MAX_VALUE)
    )

    val blockStates = CleanableThreadLocal { LongArray(2048) }

    val sectionBlocks = CleanableThreadLocal { IntArray(4096) }

    val indexStore = CleanableThreadLocal { IntArray(256) }

    val heightStore = CleanableThreadLocal { IntArray(256) }

    private val paletteCache = CleanableThreadLocal(Supplier { Palette() })

    /*
     * Vector cache
     */

    var mutableBlockVector3 = CleanableThreadLocal(Supplier { MutableBlockVector3() })

    var mutableVector3: CleanableThreadLocal<MutableVector3> = object : CleanableThreadLocal<MutableVector3>(Supplier { MutableVector3() }) {
        override fun init(): MutableVector3 {
            return MutableVector3()
        }
    }

    /*
    Palette buffers / cache
     */

    @Synchronized
    override fun trim(aggressive: Boolean): Boolean {
        if (aggressive) {
            CleanableThreadLocal.cleanAll()
        } else {
            CHUNK_FLAG.clean()
            BYTE_BUFFER_8192.clean()
            BLOCK_TO_PALETTE.clean()
            PALETTE_TO_BLOCK.clean()
            blockStates.clean()
            sectionBlocks.clean()
            paletteCache.clean()
            PALETTE_TO_BLOCK_CHAR.clean()
            indexStore.clean()

            mutableVector3.clean()
            mutableBlockVector3.clean()
            sectionBitsToChar.clean()
            for ((_, value) in REGISTERED_SINGLETONS) {
                value.clean()
            }
        }
        for ((_, pool) in REGISTERED_POOLS) {
            pool.clear()
        }

        return false
    }

    fun <T> getPool(clazz: Class<T>): Pool<T>? {
        var pool: Pool<T>? = REGISTERED_POOLS[clazz] as Pool<T>?
        if (pool == null) {
            synchronized(this) {
                pool = REGISTERED_POOLS[clazz] as Pool<T>?
                if (pool == null) {
                    getLogger(FaweCache::class.java).debug("Not registered $clazz")
                    val supplier = IOUtil.supplier<T> { clazz.newInstance() }
                    pool = Pool { supplier.get() }
                    REGISTERED_POOLS[clazz] = pool
                }
            }
        }
        return pool
    }

    fun <T> getFromPool(clazz: Class<T>): T? {
        val pool = getPool(clazz)
        return pool?.poll()
    }

    fun <T> getSingleton(clazz: Class<T>): T {
        var cache: CleanableThreadLocal<T>? = REGISTERED_SINGLETONS[clazz] as CleanableThreadLocal<T>?
        if (cache == null) {
            synchronized(this) {
                cache = REGISTERED_SINGLETONS[clazz] as CleanableThreadLocal<T>?
                if (cache == null) {
                    getLogger(FaweCache::class.java).debug("Not registered $clazz")
                    cache = CleanableThreadLocal(IOUtil.supplier { clazz.newInstance() })
                    REGISTERED_SINGLETONS[clazz] = cache
                }
            }
        }
        return cache!!.get()
    }

    @Synchronized
    fun <T> registerSingleton(clazz: Class<T>, cache: Supplier<T>): CleanableThreadLocal<T> {
        checkNotNull(cache)
        val local = CleanableThreadLocal(cache)
        val previous = (REGISTERED_SINGLETONS as java.util.Map<Class<*>, CleanableThreadLocal<*>>).putIfAbsent(clazz, local)
        check(previous == null) { "Previous key" }
        return local
    }

    @Synchronized
    fun <T> registerPool(clazz: Class<T>, cache: Supplier<T>, buffer: Boolean): Pool<T> {
        checkNotNull(cache)
        val pool: Pool<T>
        if (buffer) {
            pool = QueuePool(cache)
        } else {
            pool = Pool { cache.get() }
        }
        val previous = (REGISTERED_POOLS as java.util.Map<Class<*>, Pool<*>>).putIfAbsent(clazz, pool)
        check(previous == null) { "Previous key" }
        return pool
    }

    fun <T, V> createCache(withInitial: Supplier<V>): LoadingCache<T, V> {
        return CacheBuilder.newBuilder().build(object : CacheLoader<T, V>() {
            override fun load(key: T): V {
                return withInitial.get()
            }
        })
    }

    fun <T, V> createCache(withInitial: Function<T, V>): LoadingCache<T, V> {
        return CacheBuilder.newBuilder().build(object : CacheLoader<T, V>() {
            override fun load(key: T): V {
                return withInitial.apply(key)
            }
        })
    }

    /**
     * Holds data for a palette used in a chunk section
     */
    class Palette {
        var bitsPerEntry: Int = 0

        var paletteToBlockLength: Int = 0
        /**
         * Reusable buffer array, MUST check paletteToBlockLength for actual length
         */
        var paletteToBlock: IntArray? = null

        var blockStatesLength: Int = 0
        /**
         * Reusable buffer array, MUST check blockStatesLength for actual length
         */
        var blockStates: LongArray? = null
    }

    /**
     * Convert raw char array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    fun toPalette(layerOffset: Int, blocks: CharArray): Palette {
        return toPalette(layerOffset, null, blocks)
    }

    /**
     * Convert raw int array to palette
     * @param layerOffset
     * @param blocks
     * @return palette
     */
    fun toPalette(layerOffset: Int, blocks: IntArray): Palette {
        return toPalette(layerOffset, blocks, null)
    }

    private fun toPalette(layerOffset: Int, blocksInts: IntArray?, blocksChars: CharArray?): Palette {
        val blockToPalette = BLOCK_TO_PALETTE.get()
        val paletteToBlock = PALETTE_TO_BLOCK.get()
        val blockStates = blockStates.get()
        val blocksCopy = sectionBlocks.get()

        val blockIndexStart = layerOffset shl 12
        val blockIndexEnd = blockIndexStart + 4096
        var num_palette = 0
        try {
            if (blocksChars != null) {
                var i = blockIndexStart
                var j = 0
                while (i < blockIndexEnd) {
                    val ordinal = blocksChars[i].toInt()
                    var palette = blockToPalette[ordinal]
                    if (palette == Integer.MAX_VALUE) {
                        palette = num_palette
                        blockToPalette[ordinal] = palette
                        paletteToBlock[num_palette] = ordinal
                        num_palette++
                    }
                    blocksCopy[j] = palette
                    i++
                    j++
                }
            } else if (blocksInts != null) {
                var i = blockIndexStart
                var j = 0
                while (i < blockIndexEnd) {
                    val ordinal = blocksInts[i]
                    var palette = blockToPalette[ordinal]
                    if (palette == Integer.MAX_VALUE) {
                        //                        BlockState state = BlockTypesCache.states[ordinal];
                        palette = num_palette
                        blockToPalette[ordinal] = palette
                        paletteToBlock[num_palette] = ordinal
                        num_palette++
                    }
                    blocksCopy[j] = palette
                    i++
                    j++
                }
            } else {
                throw IllegalArgumentException()
            }

            for (i in 0 until num_palette) {
                blockToPalette[paletteToBlock[i]] = Integer.MAX_VALUE
            }

            // BlockStates
            var bitsPerEntry = MathMan.log2nlz(num_palette - 1)
            if (Settings.IMP.PROTOCOL_SUPPORT_FIX || num_palette != 1) {
                bitsPerEntry = Math.max(bitsPerEntry, 4) // Protocol support breaks <4 bits per entry
            } else {
                bitsPerEntry = Math.max(bitsPerEntry, 1) // For some reason minecraft needs 4096 bits to store 0 entries
            }
            var blockBitArrayEnd = bitsPerEntry * 4096 shr 6
            if (num_palette == 1) {
                // Set a value, because minecraft needs it for some  reason
                blockStates[0] = 0
                blockBitArrayEnd = 1
            } else {
                val bitArray = BitArray4096(blockStates, bitsPerEntry)
                bitArray.fromRaw(blocksCopy)
            }

            // Construct palette
            val palette = paletteCache.get()
            palette.bitsPerEntry = bitsPerEntry
            palette.paletteToBlockLength = num_palette
            palette.paletteToBlock = paletteToBlock

            palette.blockStatesLength = blockBitArrayEnd
            palette.blockStates = blockStates

            return palette
        } catch (e: Throwable) {
            e.printStackTrace()
            Arrays.fill(blockToPalette, Integer.MAX_VALUE)
            throw e
        }

    }

    /*
    Conversion methods between JNBT tags and raw values
     */
    fun asMap(vararg pairs: Any): Map<String, Any> {
        val map = HashMap<String, Any>(pairs.size shr 1)
        var i = 0
        while (i < pairs.size) {
            val key = pairs[i] as String
            val value = pairs[i + 1]
            map[key] = value
            i += 2
        }
        return map
    }

    fun asTag(value: Short): ShortTag {
        return ShortTag(value)
    }

    fun asTag(value: Int): IntTag {
        return IntTag(value)
    }

    fun asTag(value: Double): DoubleTag {
        return DoubleTag(value)
    }

    fun asTag(value: Byte): ByteTag {
        return ByteTag(value)
    }

    fun asTag(value: Float): FloatTag {
        return FloatTag(value)
    }

    fun asTag(value: Long): LongTag {
        return LongTag(value)
    }

    fun asTag(value: ByteArray): ByteArrayTag {
        return ByteArrayTag(value)
    }

    fun asTag(value: IntArray): IntArrayTag {
        return IntArrayTag(value)
    }

    fun asTag(value: LongArray): LongArrayTag {
        return LongArrayTag(value)
    }

    fun asTag(value: String): StringTag {
        return StringTag(value)
    }

    fun asTag(value: Map<String, Any>): CompoundTag {
        val map = HashMap<String, Tag>()
        for ((key, child) in value) {
            val tag = asTag(child)
            map[key] = tag
        }
        return CompoundTag(map)
    }

    fun asTag(value: Any?): Tag? {
        if (value is Int) {
            return asTag(value)
        } else if (value is Short) {
            return asTag(value)
        } else if (value is Double) {
            return asTag(value)
        } else if (value is Byte) {
            return asTag(value)
        } else if (value is Float) {
            return asTag(value)
        } else if (value is Long) {
            return asTag(value)
        } else if (value is String) {
            return asTag(value as String?)
        } else if (value is Map<*, *>) {
            return asTag((value as Map<String, Any>?)!!)
        } else if (value is Collection<*>) {
            return asTag((value as Collection<*>?)!!)
        } else if (value is Array<*>) {
            return asTag(*(value as Array<Any>?)!!)
        } else if (value is ByteArray) {
            return asTag(value as ByteArray?)
        } else if (value is IntArray) {
            return asTag(value as IntArray?)
        } else if (value is LongArray) {
            return asTag(value as LongArray?)
        } else if (value is Tag) {
            return value
        } else if (value is Boolean) {
            return asTag((if (value) 1 else 0).toByte())
        } else if (value == null) {
            println("Invalid nbt: " + value!!)
            return value
        } else {
            val clazz = value.javaClass
            if (clazz.name.startsWith("com.intellectualcrafters.jnbt")) {
                try {
                    if (clazz.name == "com.intellectualcrafters.jnbt.EndTag") {
                        return EndTag()
                    }
                    val field = clazz.getDeclaredField("value")
                    field.isAccessible = true
                    return asTag(field.get(value))
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
            println("Invalid nbt: $value")
            return null
        }
    }

    fun asTag(vararg values: Any): ListTag {
        var clazz: Class<out Tag>? = null
        val list = ArrayList<Tag>(values.size)
        for (value in values) {
            val tag = asTag(value)
            if (clazz == null) {
                clazz = tag.javaClass
            }
            list.add(tag)
        }
        if (clazz == null) clazz = EndTag::class.java
        return ListTag(clazz, list)
    }

    fun asTag(values: Collection<*>): ListTag {
        var clazz: Class<out Tag>? = null
        val list: ArrayList<Tag> = ArrayList<Tag>(values.size)
        for (value in values) {
            val tag = asTag(value)
            if (clazz == null) {
                clazz = tag!!.javaClass
            }
            if (tag != null) {
                list.add(tag)
            }
        }
        if (clazz == null) clazz = EndTag::class.java
        return ListTag(clazz, list)
    }

    /*
    Thread stuff
     */
    fun newBlockingExecutor(): ThreadPoolExecutor {
        val nThreads = Settings.IMP.QUEUE.PARALLEL_THREADS
        val queue = ArrayBlockingQueue<Runnable>(nThreads)
        return object : ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS, queue, Executors.defaultThreadFactory(),
                ThreadPoolExecutor.CallerRunsPolicy()) {
            override fun afterExecute(r: Runnable?, t: Throwable?) {
                var t = t
                try {
                    super.afterExecute(r, t)
                    if (t == null && r is Future<*>) {
                        try {
                            val future = r as Future<*>?
                            if (future!!.isDone) {
                                future.get()
                            }
                        } catch (ce: CancellationException) {
                            t = ce
                        } catch (ee: ExecutionException) {
                            t = ee.cause
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }

                    }
                    t?.printStackTrace()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

            }
        }
    }

    val chunk = FaweChunkLoadException()
    val blockBag = FaweBlockBagException()
    val manual = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.manual"))
    val noRegion = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.no.region"))
    val outsideRegion = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.outside.region"))
    val maxChecks = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.checks"))
    val maxChanges = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.changes"))
    val lowMemory = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.low.memory"))
    val maxEntities = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.entities"))
    val maxTiles = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.tiles"))
    val maxIterations = FaweException(TranslatableComponent.of("fawe.cancel.worldedit.cancel.reason.max.iterations"))
}
