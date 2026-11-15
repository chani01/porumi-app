package com.chani.porumi.di

import com.chani.porumi.data.AnalysisRepository
import com.chani.porumi.data.AuthRepository
import com.chani.porumi.data.PetFilter
import com.chani.porumi.data.NotificationSettingsRepository
import com.chani.porumi.data.PetRepository
import com.chani.porumi.feature.settings.AccountViewModel
import com.chani.porumi.feature.settings.MyViewModel
import com.chani.porumi.feature.settings.NotificationSettingsViewModel
import com.chani.porumi.data.createPorumiSupabase
import com.chani.porumi.feature.analyze.AnalyzeViewModel
import com.chani.porumi.feature.auth.AuthViewModel
import com.chani.porumi.feature.detail.DetailViewModel
import com.chani.porumi.feature.home.HomeViewModel
import com.chani.porumi.feature.pet.PetsViewModel
import com.chani.porumi.feature.report.ReportViewModel
import com.chani.porumi.feature.MainViewModel
import com.chani.porumi.data.NotificationRepository
import com.chani.porumi.feature.notifications.NotificationsViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val appModule = module {
    single { createPorumiSupabase() }
    single { AuthRepository(get()) }
    single { PetRepository(get()) }
    single { AnalysisRepository(get()) }
    single { PetFilter(get()) }
    single { NotificationSettingsRepository() }
    single { NotificationRepository(get()) }

    viewModelOf(::MainViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::AnalyzeViewModel)
    viewModelOf(::ReportViewModel)
    viewModelOf(::PetsViewModel)
    viewModelOf(::DetailViewModel)
    viewModelOf(::MyViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::NotificationsViewModel)
}

/** Android(Application.onCreate) / iOS(MainViewController) 에서 한 번 호출 */
fun initKoin() {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin { modules(appModule) }
}
