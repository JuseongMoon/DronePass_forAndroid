package com.ScienceFiction.DronePassAndroid.core.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.DroneDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.ShapeDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.dao.SketchDao
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.DroneEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.ShapeEntity
import com.ScienceFiction.DronePassAndroid.core.data.local.room.entity.SketchEntity

/**
 * DronePass Room 데이터베이스
 *
 * 버전 이력:
 * - v1: 초기 버전 (shapes 테이블)
 * - v2: drones, sketches 테이블 추가
 *
 * 마이그레이션 정책:
 * - 각 버전 업그레이드에 대해 Migration 객체를 정의한다.
 * - fallbackToDestructiveMigration은 개발 단계에서만 사용하고,
 *   프로덕션에서는 반드시 명시적 마이그레이션을 사용한다.
 */
@Database(
    entities = [ShapeEntity::class, DroneEntity::class, SketchEntity::class],
    version = 2,
    exportSchema = true
)
abstract class DronePassDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
    abstract fun droneDao(): DroneDao
    abstract fun sketchDao(): SketchDao

    companion object {
        /**
         * v1 -> v2 마이그레이션: drones, sketches 테이블 추가
         *
         * 참고: 현재 fallbackToDestructiveMigration()을 사용 중이므로
         *       이 마이그레이션은 직접 적용되지 않지만,
         *       프로덕션 전환 시 DatabaseModule에서 .addMigrations(MIGRATION_1_2) 로 교체한다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `drones` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `serialNumber` TEXT,
                        `takeoffWeight` TEXT,
                        `size` TEXT,
                        `memo` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `sketches` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `pathDataJson` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `strokeWidth` REAL NOT NULL,
                        `opacity` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )"""
                )
            }
        }

        /**
         * v2 -> v3 마이그레이션 (플레이스홀더)
         *
         * 향후 스키마 변경 시 여기에 SQL을 추가한다.
         * 현재는 빈 마이그레이션으로, 실제 스키마 변경이 발생할 때 구현한다.
         *
         * 예시:
         *   db.execSQL("ALTER TABLE shapes ADD COLUMN new_field TEXT")
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 향후 스키마 변경 시 여기에 SQL 추가
            }
        }

        /**
         * 모든 마이그레이션 목록
         * DatabaseModule에서 .addMigrations(*allMigrations) 으로 사용한다.
         */
        val allMigrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
