package com.boydti.fawe.database

import com.boydti.fawe.config.Config
import com.sk89q.worldedit.world.World
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

object DBHandler {
    private val log = LoggerFactory.getLogger(Config::class.java)
    private val databases: MutableMap<String, RollbackDatabase> = ConcurrentHashMap(8, 0.9f, 1)
    fun getDatabase(world: World): RollbackDatabase? {
        val worldName = world.name
        var database = databases[worldName]
        return database
                ?: try {
                    database = RollbackDatabase(world)
                    databases[worldName] = database
                    database
                } catch (e: Throwable) {
                    log.error("No JDBC driver found!\n TODO: Bundle driver with FAWE (or disable database)", e)
                    null
                }
    }

}
