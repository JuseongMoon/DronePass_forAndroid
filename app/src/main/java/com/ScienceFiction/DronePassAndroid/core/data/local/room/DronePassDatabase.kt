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
 * - v3: iOS ShapeModel geometry 필드 보존용 컬럼 복원
 *
 * 마이그레이션 정책:
 * - 각 버전 업그레이드에 대해 Migration 객체를 정의한다.
 * - fallbackToDestructiveMigration은 개발 단계에서만 사용하고,
 *   프로덕션에서는 반드시 명시적 마이그레이션을 사용한다.
 */
@Database(
    entities = [ShapeEntity::class, DroneEntity::class, SketchEntity::class],
    version = 3,
    exportSchema = true
)
abstract class DronePassDatabase : RoomDatabase() {
    abstract fun shapeDao(): ShapeDao
    abstract fun droneDao(): DroneDao
    abstract fun sketchDao(): SketchDao

    companion object {
        /**
         * v1 -> v2 마이그레이션
         *
         * 실제 스키마 변경:
         * - shapes 테이블: 4개 컬럼 제거 (secondLatitude, secondLongitude,
         *   polygonCoordinates, polylineCoordinates) — Circle 외 도형 모델 미사용
         * - drones 테이블: 신규 추가
         * - sketches 테이블: 신규 추가
         *
         * SQLite < 3.35 호환을 위해 ALTER TABLE DROP COLUMN 대신
         * 임시 테이블 → INSERT → DROP → RENAME 패턴 사용.
         *
         * 컬럼 정의는 v2 entity의 Room 자동생성 스키마(app/schemas/.../2.json)와
         * 반드시 일치해야 Room 마이그레이션 검증을 통과한다.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) shapes 테이블 재구성 (4개 컬럼 제거)
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `shapes_new` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `shapeType` TEXT NOT NULL,
                        `baseLatitude` REAL NOT NULL,
                        `baseLongitude` REAL NOT NULL,
                        `address` TEXT,
                        `radius` REAL,
                        `height` REAL,
                        `memo` TEXT,
                        `color` TEXT NOT NULL,
                        `droneId` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        `flightStartDate` INTEGER NOT NULL,
                        `flightEndDate` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
                db.execSQL(
                    """INSERT INTO `shapes_new` (
                        `id`, `title`, `shapeType`, `baseLatitude`, `baseLongitude`,
                        `address`, `radius`, `height`, `memo`, `color`, `droneId`,
                        `createdAt`, `deletedAt`, `flightStartDate`, `flightEndDate`, `updatedAt`
                    ) SELECT
                        `id`, `title`, `shapeType`, `baseLatitude`, `baseLongitude`,
                        `address`, `radius`, `height`, `memo`, `color`, `droneId`,
                        `createdAt`, `deletedAt`, `flightStartDate`, `flightEndDate`, `updatedAt`
                    FROM `shapes`""".trimIndent()
                )
                db.execSQL("DROP TABLE `shapes`")
                db.execSQL("ALTER TABLE `shapes_new` RENAME TO `shapes`")

                // 2) drones 테이블 신규 생성 (v2 entity와 동일한 컬럼 정의)
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `drones` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `serialNumber` TEXT,
                        `takeoffWeight` TEXT,
                        `size` TEXT,
                        `memo` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )

                // 3) sketches 테이블 신규 생성 (v2 entity와 동일한 컬럼 정의)
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `sketches` (
                        `id` TEXT NOT NULL,
                        `points` TEXT NOT NULL,
                        `color` TEXT NOT NULL,
                        `strokeWidth` REAL NOT NULL,
                        `opacity` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
            }
        }

        /**
         * 모든 마이그레이션 목록
         * DatabaseModule에서 .addMigrations(*allMigrations) 으로 사용한다.
         *
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `shapes` ADD COLUMN `secondLatitude` REAL")
                db.execSQL("ALTER TABLE `shapes` ADD COLUMN `secondLongitude` REAL")
                db.execSQL("ALTER TABLE `shapes` ADD COLUMN `polygonCoordinates` TEXT")
                db.execSQL("ALTER TABLE `shapes` ADD COLUMN `polylineCoordinates` TEXT")
            }
        }

        /**
         * v1 schema already contained the iOS geometry columns. Prefer the direct path so users
         * upgrading from v1 to v3 do not pass through v2 and lose those nullable columns.
         */
        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        val allMigrations = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
    }
}
