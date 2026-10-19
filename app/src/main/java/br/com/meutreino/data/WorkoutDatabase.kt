package br.com.meutreino.data

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
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

@Dao interface WorkoutDao {
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

@Database(entities=[WorkoutSessionEntity::class,WorkoutSetEntity::class,CardioActivityEntity::class],version=2,exportSchema=false)
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
