package com.mohsenoid.certhunter.di

import com.mohsenoid.certhunter.coroutine.DefaultDispatcherProvider
import com.mohsenoid.certhunter.coroutine.DispatcherProvider
import com.mohsenoid.certhunter.data.repository.AppRepositoryImpl
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.certhunter.ui.detail.AppDetailViewModel
import com.mohsenoid.certhunter.ui.list.AppListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.time.Clock

val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<Clock> { SystemDefaultZoneClock }
    single<AppRepository> {
        AppRepositoryImpl(
            packageManager = androidContext().packageManager,
            dispatcherProvider = get(),
            clock = get(),
        )
    }
    viewModel { AppListViewModel(repository = get(), dispatcherProvider = get()) }
    viewModel { (packageName: String) ->
        AppDetailViewModel(
            packageName = packageName,
            repository = get(),
            dispatcherProvider = get(),
        )
    }
}
