package br.com.meutreino.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Backup local e voluntário, sem rede nem credenciais externas.
 * Restore somente em instalação sem dados reais, para impedir sobrescrita silenciosa.
 * Não inclui Health Connect, pareamentos Bluetooth, permissões nem cache de vídeos.
 */
internal object LocalBackup {
    const val FORMAT = "br.com.meutreino.backup"
    const val VERSION = 1
    const val MAX_BYTES = 30 * 1024 * 1024

    private fun j(vararg values: Pair<String, Any?>): JSONObject = JSONObject().apply {
        for ((key, value) in values) put(key, value ?: JSONObject.NULL)
    }
    private fun <T> arr(rows: List<T>, mapper: (T) -> JSONObject): JSONArray = JSONArray().apply {
        rows.forEach { put(mapper(it)) }
    }
    private fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun JSONObject.intOrNull(k: String): Int? = if (isNull(k)) null else getInt(k)
    private fun JSONObject.doubleOrNull(k: String): Double? = if (isNull(k)) null else getDouble(k)
    private fun JSONObject.nonblank(k: String): String = getString(k).also { require(it.isNotBlank()) { "$k está vazio" } }

    // Canonicaliza chaves antes de gerar o digest, independente da ordem do JSONObject.
    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { k ->
            JSONObject.quote(k) + ":" + canonical(value.get(k))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is String -> JSONObject.quote(value)
        is Boolean, is Number -> value.toString()
        else -> error("Tipo JSON inesperado")
    }
    private fun sha256(payload: JSONObject): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical(payload).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    /** Dados materializados; nenhuma consulta a banco durante a serialização. */
    data class Snapshot(
        val sessions: List<WorkoutSessionEntity>,
        val sets: List<WorkoutSetEntity>,
        val cardio: List<CardioActivityEntity>,
        val weights: List<WeightMeasurementEntity>,
        val measurements: List<BodyMeasurementEntity>,
        val versions: List<PlanVersionEntity>,
        val recovery: List<RecoveryCheckInEntity>,
        val goals: List<BodyMeasurementGoalEntity>,
        val reviews: List<PlanReviewEventEntity>
    )

    suspend fun read(db: WorkoutDatabase): Snapshot = db.withTransaction {
        val d = db.dao()
        Snapshot(d.backupSessions(), d.backupSets(), d.backupCardio(), d.backupWeights(),
            d.backupBodyMeasurements(), d.backupPlanVersions(), d.backupRecovery(),
            d.backupGoals(), d.backupReviewEvents())
    }

    fun serialize(profile: JSONObject, data: Snapshot): String {
        val tables = j(
            "sessions" to arr(data.sessions) { j("id" to it.id,"planId" to it.planId,
                "planVersionId" to it.planVersionId,"title" to it.title,"environment" to it.environment,
                "startedAt" to it.startedAt,"finishedAt" to it.finishedAt,"status" to it.status,
                "cardioMinutes" to it.cardioMinutes,"cardioIntensity" to it.cardioIntensity) },
            "sets" to arr(data.sets) { j("id" to it.id,"sessionId" to it.sessionId,
                "exerciseId" to it.exerciseId,"exerciseName" to it.exerciseName,"setNumber" to it.setNumber,
                "repetitions" to it.repetitions,"loadKg" to it.loadKg,"rir" to it.rir,
                "recordedAt" to it.recordedAt) },
            "cardio_activities" to arr(data.cardio) { j("id" to it.id,"recordedAt" to it.recordedAt,
                "minutes" to it.minutes,"intensity" to it.intensity,"modality" to it.modality,
                "source" to it.source) },
            "weight_measurements" to arr(data.weights) { j("id" to it.id,"recordedAt" to it.recordedAt,
                "kilograms" to it.kilograms,"source" to it.source) },
            "body_measurements" to arr(data.measurements) { j("id" to it.id,"recordedAt" to it.recordedAt,
                "site" to it.site,"centimeters" to it.centimeters,"source" to it.source) },
            "plan_versions" to arr(data.versions) { j("id" to it.id,"createdAt" to it.createdAt,
                "fingerprint" to it.fingerprint,"content" to it.content,"reason" to it.reason) },
            "recovery_checkins" to arr(data.recovery) { j("id" to it.id,"planVersionId" to it.planVersionId,
                "recordedAt" to it.recordedAt,"state" to it.state) },
            "body_measurement_goals" to arr(data.goals) { j("site" to it.site,"initialCm" to it.initialCm,
                "targetCm" to it.targetCm,"startedAt" to it.startedAt) },
            "plan_review_events" to arr(data.reviews) { j("id" to it.id,"recordedAt" to it.recordedAt,
                "fromVersionId" to it.fromVersionId,"toVersionId" to it.toVersionId,
                "verdict" to it.verdict,"userReason" to it.userReason,"outcome" to it.outcome,
                "evidence" to it.evidence) }
        )
        val payload = j("profile" to profile,"tables" to tables)
        val digest = sha256(payload)
        return j("format" to FORMAT,"schema" to VERSION,"exportedAt" to System.currentTimeMillis(),
            "appVersion" to "1.0.1","payload" to payload,"sha256" to digest).toString(2)
    }

    data class Parsed(val profile: JSONObject, val snapshot: Snapshot)

    /** Valida tudo ANTES de iniciar uma transação de escrita. */
    fun parse(text: String): Parsed {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Arquivo maior que 30 MB" }
        val root = JSONObject(text)
        require(root.getString("format") == FORMAT && root.getInt("schema") == VERSION) {
            "Arquivo de backup incompatível"
        }
        val payload = root.getJSONObject("payload")
        val expected = sha256(payload)
        require(expected.equals(root.getString("sha256"), ignoreCase = true)) {
            "Backup alterado ou corrompido (SHA-256 diferente)"
        }
        val profile = payload.getJSONObject("profile")
        val t = payload.getJSONObject("tables")
        fun <T> rows(name: String, f: (JSONObject) -> T): List<T> {
            val array = t.getJSONArray(name)
            require(array.length() <= 150_000) { "Muitos registros em $name" }
            return (0 until array.length()).map { f(array.getJSONObject(it)) }
        }
        val s = Snapshot(
            sessions = rows("sessions") { WorkoutSessionEntity(
                id=it.getLong("id"), planId=it.nonblank("planId"),
                planVersionId=it.longOrNull("planVersionId"), title=it.nonblank("title"),
                environment=it.nonblank("environment"), startedAt=it.getLong("startedAt"),
                finishedAt=it.longOrNull("finishedAt"), status=it.getString("status"),
                cardioMinutes=it.getInt("cardioMinutes"), cardioIntensity=it.getString("cardioIntensity")) },
            sets = rows("sets") { WorkoutSetEntity(
                id=it.getLong("id"), sessionId=it.getLong("sessionId"),
                exerciseId=it.nonblank("exerciseId"),exerciseName=it.nonblank("exerciseName"),
                setNumber=it.getInt("setNumber"), repetitions=it.getInt("repetitions"),
                loadKg=it.doubleOrNull("loadKg"), rir=it.intOrNull("rir"),
                recordedAt=it.getLong("recordedAt")) },
            cardio = rows("cardio_activities") { CardioActivityEntity(
                id=it.nonblank("id"), recordedAt=it.getLong("recordedAt"),
                minutes=it.getInt("minutes"),intensity=it.nonblank("intensity"),
                modality=it.nonblank("modality"),source=it.getString("source")) },
            weights = rows("weight_measurements") { WeightMeasurementEntity(
                id=it.getLong("id"),recordedAt=it.getLong("recordedAt"),
                kilograms=it.getDouble("kilograms"),source=it.getString("source")) },
            measurements = rows("body_measurements") { BodyMeasurementEntity(
                id=it.getLong("id"),recordedAt=it.getLong("recordedAt"),
                site=it.nonblank("site"),centimeters=it.getDouble("centimeters"),
                source=it.getString("source")) },
            versions = rows("plan_versions") { PlanVersionEntity(
                id=it.getLong("id"),createdAt=it.getLong("createdAt"),
                fingerprint=it.nonblank("fingerprint"),content=it.nonblank("content"),
                reason=it.getString("reason")) },
            recovery = rows("recovery_checkins") { RecoveryCheckInEntity(
                id=it.getLong("id"),planVersionId=it.longOrNull("planVersionId"),
                recordedAt=it.getLong("recordedAt"),state=it.nonblank("state")) },
            goals = rows("body_measurement_goals") { BodyMeasurementGoalEntity(
                site=it.nonblank("site"),initialCm=it.getDouble("initialCm"),
                targetCm=it.getDouble("targetCm"),startedAt=it.getLong("startedAt")) },
            reviews = rows("plan_review_events") { PlanReviewEventEntity(
                id=it.getLong("id"),recordedAt=it.getLong("recordedAt"),
                fromVersionId=it.longOrNull("fromVersionId"),
                toVersionId=it.longOrNull("toVersionId"),
                verdict=it.nonblank("verdict"),userReason=it.getString("userReason"),
                outcome=it.nonblank("outcome"),evidence=it.getString("evidence")) }
        )
        validate(s)
        return Parsed(profile, s)
    }

    private fun <T> unique(items: List<T>, name: String) {
        require(items.size == items.toSet().size) { "IDs duplicados: $name" }
    }
    private fun validate(s: Snapshot) {
        unique(s.sessions.map { it.id },"sessões")
        unique(s.sets.map { it.id },"séries")
        unique(s.cardio.map { it.id },"cardio")
        unique(s.weights.map { it.id },"pesagens")
        unique(s.measurements.map { it.id },"medidas")
        unique(s.versions.map { it.id },"fichas")
        unique(s.versions.map { it.fingerprint },"fichas-fingerprint")
        unique(s.recovery.map { it.id },"recuperação")
        unique(s.goals.map { it.site },"metas")
        unique(s.reviews.map { it.id },"revisões")
        val sessionIds = s.sessions.map { it.id }.toSet()
        val versionIds = s.versions.map { it.id }.toSet()
        require(s.sessions.all { it.id > 0 && it.startedAt > 0 && it.cardioMinutes >= 0 &&
            (it.planVersionId == null || it.planVersionId in versionIds) }) { "Sessões inválidas" }
        require(s.sets.all { it.id > 0 && it.sessionId in sessionIds && it.repetitions > 0 &&
            it.setNumber > 0 && (it.loadKg == null || it.loadKg.isFinite() && it.loadKg >= 0) }) {
            "Séries inválidas ou sem sessão correspondente"
        }
        require(s.versions.all { it.id > 0 && it.createdAt > 0 }) { "Fichas inválidas" }
        require(s.weights.all { it.id > 0 && it.kilograms.isFinite() && it.kilograms > 0 }) { "Pesagens inválidas" }
        require(s.measurements.all { it.id > 0 && it.centimeters.isFinite() && it.centimeters > 0 }) {
            "Medidas inválidas"
        }
        require(s.cardio.all { it.minutes > 0 && it.recordedAt > 0 }) { "Cardio inválido" }
        require(s.recovery.all { it.id > 0 && (it.planVersionId == null || it.planVersionId in versionIds) }) {
            "Recuperação inválida"
        }
        require(s.goals.all { it.initialCm.isFinite() && it.targetCm.isFinite() &&
            it.initialCm > 0 && it.targetCm > 0 }) { "Metas inválidas" }
        require(s.reviews.all { it.id > 0 && (it.fromVersionId == null || it.fromVersionId in versionIds) &&
            (it.toVersionId == null || it.toVersionId in versionIds) }) { "Revisões inválidas" }
    }

    /** Exclusivamente sem dados reais; gravação Room transacional, preservando IDs e FKs. */
    suspend fun restore(db: WorkoutDatabase, snapshot: Snapshot) = db.withTransaction {
        val d = db.dao()
        check(d.countNonPlanRecords() == 0 && d.countPlanVersions() <= 1) {
            "Este celular já contém registros ou múltiplas fichas. A restauração não mescla históricos existentes."
        }
        d.clearInitialPlanVersionsForRestore()
        snapshot.versions.forEach { check(d.insertPlanVersion(it) > 0) { "Ficha duplicada" } }
        snapshot.sessions.forEach { d.insertSession(it) }
        snapshot.sets.forEach { d.insertSet(it) }
        snapshot.cardio.forEach { d.insertCardio(it) }
        snapshot.weights.forEach { d.insertWeight(it) }
        snapshot.measurements.forEach { d.insertBodyMeasurement(it) }
        snapshot.recovery.forEach { d.insertRecoveryCheckIn(it) }
        snapshot.goals.forEach { d.upsertBodyGoal(it) }
        snapshot.reviews.forEach { d.insertReviewEvent(it) }
    }
}
