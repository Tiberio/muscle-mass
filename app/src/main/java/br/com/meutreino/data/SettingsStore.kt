package br.com.meutreino.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.meutreino.domain.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.userDataStore by preferencesDataStore("settings_v1")

/** JSON é armazenado como valor único no DataStore, sem depender de nuvem. */
class SettingsStore(private val context: Context) {
    private val key = stringPreferencesKey("profile_json")
    val data: Flow<UserPrefs> = context.userDataStore.data.map { values ->
        try { decode(JSONObject(values[key] ?: "{}")) } catch (_: Exception) { UserPrefs() }
    }
    suspend fun save(p: UserPrefs) { context.userDataStore.edit { it[key] = encode(p).toString() } }

    private fun list(obj: JSONObject, key: String): List<String> = obj.optJSONArray(key)?.let { a ->
        (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
    } ?: emptyList()
    private fun map(obj: JSONObject, key: String): Map<String,String> = obj.optJSONObject(key)?.let { o ->
        o.keys().asSequence().associateWith { o.optString(it) }
    } ?: emptyMap()
    private fun encode(p: UserPrefs) = JSONObject().apply {
        put("age", p.age); put("heightCm",p.heightCm);put("weightKg",p.weightKg)
        put("experience",p.experience);put("regularity",p.regularity);put("limitations",p.limitations)
        put("objective",p.objective);put("secondaryGoals",JSONArray(p.secondaryGoals))
        put("priorityMuscles",JSONArray(p.priorityMuscles));put("environments",JSONArray(p.environments))
        put("defaultEnvironment",p.defaultEnvironment);put("equipments",JSONArray(p.equipments))
        put("scheduleMode",p.scheduleMode);put("weekdays",JSONArray(p.weekdays))
        put("weeklyCount",p.weeklyCount);put("minutesPerSession",p.minutesPerSession)
        put("preferredTime",p.preferredTime);put("flexibleDuration",p.flexibleDuration)
        put("cardioActivities",JSONArray(p.cardioActivities));put("cardioWhen",p.cardioWhen)
        put("cardioMinutes",p.cardioMinutes);put("cardioWeeklyDays",p.cardioWeeklyDays);put("cardioIntensity",p.cardioIntensity)
        put("deviceNames",JSONArray(p.deviceNames));put("deviceApp",p.deviceApp)
        put("importHistory",p.importHistory)
        put("overrides",JSONObject(p.overrides));put("assignments",JSONObject(p.assignments))
        put("onboardingCompleted",p.onboardingCompleted);put("planAccepted",p.planAccepted)
    }
    private fun decode(o: JSONObject) = UserPrefs(
        age=o.optString("age"),heightCm=o.optString("heightCm"),weightKg=o.optString("weightKg"),
        experience=o.optString("experience","Iniciante"),regularity=o.optString("regularity","Retornando / começando"),
        limitations=o.optString("limitations"),objective=o.optString("objective","Saúde e bem-estar"),
        secondaryGoals=list(o,"secondaryGoals"),priorityMuscles=list(o,"priorityMuscles"),
        environments=list(o,"environments"),defaultEnvironment=o.optString("defaultEnvironment"),
        equipments=list(o,"equipments"),scheduleMode=o.optString("scheduleMode","Dias definidos"),
        weekdays=list(o,"weekdays"),weeklyCount=o.optInt("weeklyCount",3),
        minutesPerSession=o.optInt("minutesPerSession",45),preferredTime=o.optString("preferredTime","Sem preferência"),
        flexibleDuration=o.optBoolean("flexibleDuration",false),cardioActivities=list(o,"cardioActivities"),
        cardioWhen=o.optString("cardioWhen","Misto"),cardioMinutes=o.optInt("cardioMinutes",15),cardioWeeklyDays=o.optInt("cardioWeeklyDays",0),
        cardioIntensity=o.optString("cardioIntensity","Adaptativa"),deviceNames=list(o,"deviceNames"),
        deviceApp=o.optString("deviceApp","Zepp Life"),importHistory=o.optBoolean("importHistory",false),
        overrides=map(o,"overrides"),assignments=map(o,"assignments"),
        onboardingCompleted=o.optBoolean("onboardingCompleted",false),planAccepted=o.optBoolean("planAccepted",false)
    )
}
