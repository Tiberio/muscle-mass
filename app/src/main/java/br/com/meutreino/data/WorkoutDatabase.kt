package br.com.meutreino.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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
    val cardioMinutes: Int = 0
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

@Dao interface WorkoutDao {
    @Insert suspend fun insertSession(session: WorkoutSessionEntity): Long
    @Insert suspend fun insertSet(set: WorkoutSetEntity): Long
    @Query("UPDATE sessions SET status='COMPLETED', finishedAt=:time, cardioMinutes=:cardio WHERE id=:id")
    suspend fun finishSession(id:Long,time:Long,cardio:Int)
    @Query("SELECT * FROM sessions WHERE status='ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun activeSession(): WorkoutSessionEntity?
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<WorkoutSessionEntity>>
    @Query("SELECT * FROM sets WHERE sessionId=:id ORDER BY recordedAt ASC,id ASC")
    fun observeSets(id:Long): Flow<List<WorkoutSetEntity>>
    @Query("SELECT * FROM sets ORDER BY recordedAt DESC")
    fun observeAllSets(): Flow<List<WorkoutSetEntity>>
}

@Database(entities=[WorkoutSessionEntity::class,WorkoutSetEntity::class],version=1,exportSchema=false)
abstract class WorkoutDatabase: RoomDatabase() {
    abstract fun dao(): WorkoutDao
}
