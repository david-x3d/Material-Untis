package dev.x3d.dayline.di

import dev.x3d.dayline.data.repo.PeriodRepositoryImpl
import dev.x3d.dayline.sync.SyncScheduler
import dev.x3d.dayline.sync.TimetableSyncWorker
import dev.x3d.dayline.ui.absences.AbsencesViewModel
import dev.x3d.dayline.ui.lesson.LessonViewModel
import dev.x3d.dayline.ui.login.LoginViewModel
import dev.x3d.dayline.ui.login.QrDraftStore
import dev.x3d.dayline.ui.onboarding.OnboardingDraft
import dev.x3d.dayline.ui.school.SchoolViewModel
import dev.x3d.dayline.ui.settings.SettingsViewModel
import dev.x3d.dayline.ui.timetable.TimetableViewModel
import dev.x3d.dayline.wear.WearableWatchPusher
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<PeriodRepositoryImpl.WatchPusher> { WearableWatchPusher(androidContext()) }
    single { SyncScheduler(androidContext()) }
    single { QrDraftStore() }
    single { OnboardingDraft() }
    viewModel { SchoolViewModel(get()) }
    viewModel { LoginViewModel(get(), get(), get(), get()) }
    viewModel { TimetableViewModel(get()) }
    viewModel { AbsencesViewModel(get()) }
    viewModel { LessonViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    worker { TimetableSyncWorker(get(), get()) }
}
