package br.com.meutreino.data

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Delete
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName="sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val planId: String,
    val planVersionId: Long? = null,
    val title: String,
    val environment: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: String = "ACTIVE",
    val cardioMinutes: Int = 0,
    @ColumnInfo(defaultValue = "'UNKNOWN'") val cardioIntensity: String = "UNKNOWN"
)

@Entity(tableName="sets", foreignKeys=[ForeignKey(entity=WorkoutSessionEntity::class,
    parentColumns=["id"], childColumns=["sessionId"],onDelete=ForeignKey.CASCADE)],
    indices=[Index("sessionId"),Index("exerciseId")])
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val repetitions: Int,
    val loadKg: Double?,
    val rir: Int?,
    val recordedAt: Long
)

@Entity(tableName="cardio_activities", indices=[Index(value=["recordedAt"])])
data class CardioActivityEntity(
    @PrimaryKey val id: String,
    val recordedAt: Long,
    val minutes: Int,
    val intensity: String,
    val modality: String,
    val source: String = "manual"
)

@Entity(tableName="weight_measurements", indices=[Index(value=["recordedAt"])])
data class WeightMeasurementEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val recordedAt: Long,
    val kilograms: Double,
    val source: String
)

/** Medidas inseridas pelo usuário; nunca inferir circunferência a partir de peso. */
@Entity(tableName="body_measurements", indices=[Index(value=["site", "recordedAt"])])
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val recordedAt: Long,
    val site: String,
    val centimeters: Double,
    val source: String = "manual"
)

/** Versões imutáveis do plano gerado, com conteúdo textual para consulta offline. */
@Entity(tableName="plan_versions", indices=[Index(value=["fingerprint"], unique=true)])
data class PlanVersionEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val createdAt: Long,
    val fingerprint: String,
    val content: String,
    val reason: String
)

/** Check-in voluntário. Não representa diagnóstico ou triagem clínica. */
@Entity(tableName="recovery_checkins", indices=[Index(value=["recordedAt"])])
data class RecoveryCheckInEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val planVersionId: Long?,
    val recordedAt: Long,
    val state: String
)

/** Meta explícita por circunferência. Não confundir com completude de registros. */
@Entity(tableName="body_measurement_goals")
data class BodyMeasurementGoalEntity(
    @PrimaryKey val site: String,
    val initialCm: Double,
    val targetCm: Double,
    val startedAt: Long
)

/** Trilha de decisões de revisão; a ficha não muda até aplicação final explícita. */
@Entity(tableName="plan_review_events", indices=[Index(value=["recordedAt"])])
data class PlanReviewEventEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val recordedAt: Long,
    val fromVersionId: Long?,
    val toVersionId: Long?,
    val verdict: String,
    val userReason: String,
    val outcome: String,
    val evidence: String
)

@Dao interface WorkoutDao {
    // v1.0.0 — fotografia consistente do banco para backup voluntário.
    @Query("SELECT * FROM sessions ORDER BY id") suspend fun backupSessions(): List<WorkoutSessionEntity>
    @Query("SELECT * FROM sets ORDER BY id") suspend fun backupSets(): List<WorkoutSetEntity>
    @Query("SELECT * FROM cardio_activities ORDER BY id") suspend fun backupCardio(): List<CardioActivityEntity>
    @Query("SELECT * FROM weight_measurements ORDER BY id") suspend fun backupWeights(): List<WeightMeasurementEntity>
    @Query("SELECT * FROM body_measurements ORDER BY id") suspend fun backupBodyMeasurements(): List<BodyMeasurementEntity>
    @Query("SELECT * FROM plan_versions ORDER BY id") suspend fun backupPlanVersions(): List<PlanVersionEntity>
    @Query("SELECT * FROM recovery_checkins ORDER BY id") suspend fun backupRecovery(): List<RecoveryCheckInEntity>
    @Query("SELECT * FROM body_measurement_goals ORDER BY site") suspend fun backupGoals(): List<BodyMeasurementGoalEntity>
    @Query("SELECT * FROM plan_review_events ORDER BY id") suspend fun backupReviewEvents(): List<PlanReviewEventEntity>

    // Importação de backup: só permitida se não houver registros reais.
    // Uma ficha inicial gerada durante o onboarding pode ser descartada em favor do backup.
    @Query("""SELECT
        (SELECT COUNT(*) FROM sessions) + (SELECT COUNT(*) FROM sets) +
        (SELECT COUNT(*) FROM cardio_activities) + (SELECT COUNT(*) FROM weight_measurements) +
        (SELECT COUNT(*) FROM body_measurements) + (SELECT COUNT(*) FROM recovery_checkins) +
        (SELECT COUNT(*) FROM body_measurement_goals) + (SELECT COUNT(*) FROM plan_review_events)
        """) suspend fun countNonPlanRecords(): Int
    @Query("SELECT COUNT(*) FROM plan_versions") suspend fun countPlanVersions(): Int
    @Query("DELETE FROM plan_versions") suspend fun clearInitialPlanVersionsForRestore()
    @Insert suspend fun insertRecoveryCheckIn(entry: RecoveryCheckInEntity): Long
    @Query("SELECT * FROM recovery_checkins ORDER BY recordedAt DESC, id DESC")
    fun observeRecoveryCheckIns(): Flow<List<RecoveryCheckInEntity>>
    @Query("SELECT * FROM recovery_checkins WHERE planVersionId=:versionId ORDER BY recordedAt DESC, id DESC LIMIT 1")
    suspend fun latestRecovery(versionId:Long): RecoveryCheckInEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun upsertBodyGoal(goal: BodyMeasurementGoalEntity)
    @Delete suspend fun deleteBodyGoal(goal: BodyMeasurementGoalEntity)
    @Query("SELECT * FROM body_measurement_goals ORDER BY site")
    fun observeBodyGoals(): Flow<List<BodyMeasurementGoalEntity>>
    @Insert suspend fun insertReviewEvent(event: PlanReviewEventEntity): Long
    @Query("SELECT * FROM plan_review_events ORDER BY recordedAt DESC, id DESC")
    fun observeReviewEvents(): Flow<List<PlanReviewEventEntity>>
    @Insert suspend fun insertBodyMeasurement(measurement: BodyMeasurementEntity): Long
    @Delete suspend fun deleteBodyMeasurement(measurement: BodyMeasurementEntity)
    @Query("SELECT * FROM body_measurements ORDER BY recordedAt DESC, id DESC")
    fun observeBodyMeasurements(): Flow<List<BodyMeasurementEntity>>
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertPlanVersion(version: PlanVersionEntity): Long
    @Query("SELECT * FROM plan_versions WHERE fingerprint=:fingerprint LIMIT 1")
    suspend fun findPlanVersion(fingerprint: String): PlanVersionEntity?
    @Query("SELECT * FROM plan_versions ORDER BY createdAt DESC, id DESC")
    fun observePlanVersions(): Flow<List<PlanVersionEntity>>
    @Insert suspend fun insertWeight(measurement: WeightMeasurementEntity): Long
    @Query("SELECT * FROM weight_measurements ORDER BY recordedAt DESC, id DESC")
    fun observeWeights(): Flow<List<WeightMeasurementEntity>>
    @Insert suspend fun insertSession(session: WorkoutSessionEntity): Long
    @Insert suspend fun insertSet(set: WorkoutSetEntity): Long
    @Insert suspend fun insertCardio(activity: CardioActivityEntity)
    @Query("SELECT * FROM cardio_activities ORDER BY recordedAt DESC")
    fun observeCardio(): Flow<List<CardioActivityEntity>>
    @Query("DELETE FROM sessions WHERE id=:id")
    suspend fun deleteSession(id: Long)
    @Query("UPDATE sessions SET status='COMPLETED', finishedAt=:time, cardioMinutes=:cardio, cardioIntensity=:intensity WHERE id=:id")
    suspend fun finishSession(id:Long,time:Long,cardio:Int,intensity:String)
    @Query("SELECT * FROM sessions WHERE status='ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): WorkoutSessionEntity?
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT * FROM sets WHERE sessionId=:id ORDER BY recordedAt ASC,id ASC")
    fun observeSets(id:Long): Flow<List<WorkoutSetEntity>>
    @Query("SELECT * FROM sets ORDER BY recordedAt DESC")
    fun observeAllSets(): Flow<List<WorkoutSetEntity>>
}

@Database(entities=[WorkoutSessionEntity::class,WorkoutSetEntity::class,CardioActivityEntity::class,WeightMeasurementEntity::class,BodyMeasurementEntity::class,PlanVersionEntity::class,RecoveryCheckInEntity::class,BodyMeasurementGoalEntity::class,PlanReviewEventEntity::class],version=5,exportSchema=false)
abstract class WorkoutDatabase: RoomDatabase() {
    abstract fun dao(): WorkoutDao
}

/** Migração explícita: preserva todas as sessões e séries da v0.1.1. */
val MIGRATION_1_2 = object: Migration(1,2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN cardioIntensity TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("CREATE TABLE IF NOT EXISTS cardio_activities (`id` TEXT NOT NULL, `recordedAt` INTEGER NOT NULL, `minutes` INTEGER NOT NULL, `intensity` TEXT NOT NULL, `modality` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_cardio_activities_recordedAt ON cardio_activities (`recordedAt`)")
    }
}

/** A migração 2→3 adiciona somente pesagens confirmadas, preservando históricos antigos. */
val MIGRATION_2_3 = object: Migration(2,3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordedAt` INTEGER NOT NULL, `kilograms` REAL NOT NULL, `source` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_measurements_recordedAt` ON `weight_measurements` (`recordedAt`)")
    }
}


/** v0.5.0: migração aditiva 3→4; não apaga sessões, séries, cardio nem pesagens. */
val MIGRATION_3_4 = object: Migration(3,4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sessions ADD COLUMN planVersionId INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS `body_measurements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordedAt` INTEGER NOT NULL, `site` TEXT NOT NULL, `centimeters` REAL NOT NULL, `source` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_body_measurements_site_recordedAt` ON `body_measurements` (`site`, `recordedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `plan_versions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `createdAt` INTEGER NOT NULL, `fingerprint` TEXT NOT NULL, `content` TEXT NOT NULL, `reason` TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plan_versions_fingerprint` ON `plan_versions` (`fingerprint`)")
    }
}

/** v0.6.0: migração aditiva 4→5, preserva os dados clínicos e de treino existentes. */
val MIGRATION_4_5 = object: Migration(4,5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `recovery_checkins` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `planVersionId` INTEGER, `recordedAt` INTEGER NOT NULL, `state` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recovery_checkins_recordedAt` ON `recovery_checkins` (`recordedAt`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `body_measurement_goals` (`site` TEXT NOT NULL, `initialCm` REAL NOT NULL, `targetCm` REAL NOT NULL, `startedAt` INTEGER NOT NULL, PRIMARY KEY(`site`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `plan_review_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordedAt` INTEGER NOT NULL, `fromVersionId` INTEGER, `toVersionId` INTEGER, `verdict` TEXT NOT NULL, `userReason` TEXT NOT NULL, `outcome` TEXT NOT NULL, `evidence` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_review_events_recordedAt` ON `plan_review_events` (`recordedAt`)")
    }
}
