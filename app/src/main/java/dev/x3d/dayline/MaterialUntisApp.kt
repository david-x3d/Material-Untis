package dev.x3d.dayline

import android.app.Application
import dev.x3d.dayline.di.appModule
import dev.x3d.dayline.di.coreModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class MaterialUntisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MaterialUntisApp)
            workManagerFactory()
            modules(coreModule, appModule)
        }
    }
}
