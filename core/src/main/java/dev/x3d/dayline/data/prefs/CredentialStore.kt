package dev.x3d.dayline.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.x3d.dayline.domain.model.AuthKind
import dev.x3d.dayline.domain.model.School
import dev.x3d.dayline.domain.model.UserSession

class CredentialStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context)

    var school: School?
        get() {
            val host = prefs.getString(KEY_HOST, null) ?: return null
            val login = prefs.getString(KEY_SCHOOL, null) ?: return null
            return School(
                displayName = prefs.getString(KEY_SCHOOL_DISPLAY, login) ?: login,
                loginName = login,
                host = host,
                address = prefs.getString(KEY_SCHOOL_ADDRESS, "").orEmpty(),
            )
        }
        set(value) {
            prefs.edit()
                .putString(KEY_HOST, value?.host)
                .putString(KEY_SCHOOL, value?.loginName)
                .putString(KEY_SCHOOL_DISPLAY, value?.displayName)
                .putString(KEY_SCHOOL_ADDRESS, value?.address)
                .apply()
        }

    var username: String?
        get() = prefs.getString(KEY_USER, null)
        set(value) { prefs.edit().putString(KEY_USER, value).apply() }

    var authKind: AuthKind?
        get() = prefs.getString(KEY_AUTH_KIND, null)?.let { runCatching { AuthKind.valueOf(it) }.getOrNull() }
        set(value) { prefs.edit().putString(KEY_AUTH_KIND, value?.name).apply() }

    fun secretOrPassword(): String? = prefs.getString(KEY_SECRET, null)

    fun saveCredentials(user: String, secretOrPassword: String, kind: AuthKind) {
        prefs.edit()
            .putString(KEY_USER, user)
            .putString(KEY_SECRET, secretOrPassword)
            .putString(KEY_AUTH_KIND, kind.name)
            .apply()
    }

    var session: UserSession?
        get() {
            val host = prefs.getString(KEY_HOST, null) ?: return null
            val schoolName = prefs.getString(KEY_SCHOOL, null) ?: return null
            val user = prefs.getString(KEY_USER, null) ?: return null
            if (!prefs.contains(KEY_PERSON_ID)) return null
            return UserSession(
                host = host,
                school = schoolName,
                schoolDisplayName = prefs.getString(KEY_SCHOOL_DISPLAY, schoolName) ?: schoolName,
                user = user,
                personType = prefs.getInt(KEY_PERSON_TYPE, UserSession.PERSON_STUDENT),
                personId = prefs.getInt(KEY_PERSON_ID, 0),
                klasseId = if (prefs.contains(KEY_KLASSE_ID)) prefs.getInt(KEY_KLASSE_ID, 0) else null,
            )
        }
        set(value) {
            val editor = prefs.edit()
            if (value == null) {
                editor.remove(KEY_PERSON_ID).remove(KEY_PERSON_TYPE).remove(KEY_KLASSE_ID)
            } else {
                editor
                    .putInt(KEY_PERSON_ID, value.personId)
                    .putInt(KEY_PERSON_TYPE, value.personType)
                    .apply {
                        if (value.klasseId != null) putInt(KEY_KLASSE_ID, value.klasseId) else remove(KEY_KLASSE_ID)
                    }
            }
            editor.apply()
        }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE = "dayline_creds"
        private const val KEY_HOST = "host"
        private const val KEY_SCHOOL = "school"
        private const val KEY_SCHOOL_DISPLAY = "school_display"
        private const val KEY_SCHOOL_ADDRESS = "school_address"
        private const val KEY_USER = "user"
        private const val KEY_SECRET = "secret"
        private const val KEY_AUTH_KIND = "auth_kind"
        private const val KEY_PERSON_ID = "person_id"
        private const val KEY_PERSON_TYPE = "person_type"
        private const val KEY_KLASSE_ID = "klasse_id"

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                encrypted(context)
            } catch (_: Exception) {
                runCatching {
                    context.deleteSharedPreferences(FILE)
                    encrypted(context)
                }.getOrElse {
                    context.getSharedPreferences("${FILE}_plain", Context.MODE_PRIVATE)
                }
            }
        }

        private fun encrypted(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
