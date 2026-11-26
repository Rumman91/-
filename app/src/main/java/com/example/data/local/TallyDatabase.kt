package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [CustomerEntity::class, TransactionEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class TallyDatabase : RoomDatabase() {

  abstract fun tallyDao(): TallyDao

  companion object {
    @Volatile private var INSTANCE: TallyDatabase? = null

    fun getInstance(context: Context): TallyDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                TallyDatabase::class.java,
                "tally_khata_db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
