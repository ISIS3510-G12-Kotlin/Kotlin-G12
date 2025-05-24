package com.example.explorandes.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.explorandes.database.dao.*
import com.example.explorandes.database.entity.*
import com.example.explorandes.models.VisitedItem
import com.example.explorandes.database.typeconverters.DateConverter

@Database(
    entities = [
        BuildingEntity::class,
        PlaceEntity::class,
        EventEntity::class,
        EventDetailEntity::class,
        VisitedItem::class // ✅ Se incluye el historial de eventos visitados
    ],
    version = 3, // ✅ Asegúrate de que esto sea mayor al valor anterior si hubo cambios
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun buildingDao(): BuildingDao
    abstract fun placeDao(): PlaceDao
    abstract fun eventDao(): EventDao
    abstract fun eventDetailDao(): EventDetailDao
    abstract fun visitedDao(): VisitedDao // ✅ Dao del historial

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "explorandes_db"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
