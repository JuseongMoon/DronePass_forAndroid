package com.ScienceFiction.DronePassAndroid.core.di

import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.remote.NaverGeocodingApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.DroneFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.ShapeFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.remote.firebase.SketchFirebaseStore
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpGfzApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaa27DayApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.kp.KpNoaaApi
import com.ScienceFiction.DronePassAndroid.core.data.remote.vworld.VWorldApi
import com.ScienceFiction.DronePassAndroid.core.data.repository.DroneRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.GeocodingRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.KpIndexRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.ShapeRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.SketchRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.VWorldRepository
import com.ScienceFiction.DronePassAndroid.core.data.repository.WeatherRepository
import com.ScienceFiction.DronePassAndroid.core.data.remote.weather.WeatherApi
import com.ScienceFiction.DronePassAndroid.core.data.sync.RealtimeSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideShapeRepository(
        shapeDao: ShapeDao,
        shapeFirebaseStore: ShapeFirebaseStore,
        firebaseAuth: FirebaseAuth
    ): ShapeRepository {
        return ShapeRepository(shapeDao, shapeFirebaseStore, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideDroneRepository(
        droneDao: DroneDao,
        droneFirebaseStore: DroneFirebaseStore,
        firebaseAuth: FirebaseAuth
    ): DroneRepository {
        return DroneRepository(droneDao, droneFirebaseStore, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideSketchRepository(
        sketchDao: SketchDao,
        sketchFirebaseStore: SketchFirebaseStore,
        firebaseAuth: FirebaseAuth
    ): SketchRepository {
        return SketchRepository(sketchDao, sketchFirebaseStore, firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideGeocodingRepository(api: NaverGeocodingApi): GeocodingRepository {
        return GeocodingRepository(api)
    }

    @Provides
    @Singleton
    fun provideRealtimeSyncManager(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        shapeRepository: ShapeRepository,
        droneRepository: DroneRepository,
        sketchRepository: SketchRepository
    ): RealtimeSyncManager {
        return RealtimeSyncManager(firestore, auth, shapeRepository, droneRepository, sketchRepository)
    }

    @Provides
    @Singleton
    fun provideVWorldRepository(
        vWorldApi: VWorldApi,
        @Named("VWorldApiKey") apiKey: String
    ): VWorldRepository {
        return VWorldRepository(vWorldApi, apiKey)
    }

    @Provides
    @Singleton
    fun provideKpIndexRepository(
        gfzApi: KpGfzApi,
        noaaApi: KpNoaaApi,
        noaa27DayApi: KpNoaa27DayApi
    ): KpIndexRepository {
        return KpIndexRepository(gfzApi, noaaApi, noaa27DayApi)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        weatherApi: WeatherApi
    ): WeatherRepository {
        return WeatherRepository(weatherApi)
    }
}
