package dev.x3d.dayline

import android.app.Application
import dev.x3d.dayline.data.WearTimetableStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@WearApp)
            modules(
                module {
                    single { WearTimetableStore(androidContext()) }
                },
            )
        }
    }
}
