package dev.x3d.dayline.di

import dev.x3d.dayline.data.db.PeriodDatabase
import dev.x3d.dayline.data.prefs.CredentialStore
import dev.x3d.dayline.data.prefs.UserPrefs
import dev.x3d.dayline.data.repo.PeriodRepositoryImpl
import dev.x3d.dayline.data.rpc.SchoolSearchClient
import dev.x3d.dayline.data.rpc.SessionCookieJar
import dev.x3d.dayline.data.rpc.WebUntisClient
import dev.x3d.dayline.domain.PeriodRepository
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
    single { SessionCookieJar() }
    single {
        OkHttpClient.Builder()
            .cookieJar(get<SessionCookieJar>())
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
    single { WebUntisClient(http = get(), json = get(), cookieJar = get()) }
    single { SchoolSearchClient(http = get(), json = get()) }
    single { PeriodDatabase.create(androidContext()) }
    single { get<PeriodDatabase>().lessonDao() }
    single { get<PeriodDatabase>().timegridDao() }
    single { get<PeriodDatabase>().metaDao() }
    single { CredentialStore(androidContext()) }
    single { UserPrefs(androidContext()) }
    single<PeriodRepository> {
        PeriodRepositoryImpl(
            client = get(),
            schoolSearch = get(),
            lessonDao = get(),
            timegridDao = get(),
            metaDao = get(),
            credentials = get(),
            prefs = get(),
            watchPusher = getOrNull<PeriodRepositoryImpl.WatchPusher>(),
        )
    }
}
