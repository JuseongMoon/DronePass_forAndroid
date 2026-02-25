package com.ScienceFiction.DronePassAndroid.core.di

import android.content.Context
import androidx.room.Room
import com.ScienceFiction.DronePassAndroid.core.data.local.room.DronePassDatabase
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DronePassDatabase {
        return Room.databaseBuilder(
            context,
            DronePassDatabase::class.java,
            "dronepass_database"
        )
            // 명시적 마이그레이션만 사용 (데이터 손실 방지)
            .addMigrations(*DronePassDatabase.allMigrations)
            .build()
    }

    @Provides
    fun provideShapeDao(database: DronePassDatabase): ShapeDao = database.shapeDao()

    @Provides
    fun provideDroneDao(database: DronePassDatabase): DroneDao = database.droneDao()

    @Provides
    fun provideSketchDao(database: DronePassDatabase): SketchDao = database.sketchDao()
}
