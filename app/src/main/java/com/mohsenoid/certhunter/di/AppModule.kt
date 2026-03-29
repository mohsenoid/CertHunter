package com.mohsenoid.certhunter.di

import com.mohsenoid.certhunter.data.repository.AppRepositoryImpl
import com.mohsenoid.certhunter.domain.repository.AppRepository
import com.mohsenoid.certhunter.ui.detail.AppDetailViewModel
import com.mohsenoid.certhunter.ui.list.AppListViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<AppRepository> { AppRepositoryImpl(androidContext().packageManager) }
    viewModel { AppListViewModel(repository = get()) }
    viewModel { (packageName: String) -> AppDetailViewModel(packageName = packageName, repository = get()) }
}
