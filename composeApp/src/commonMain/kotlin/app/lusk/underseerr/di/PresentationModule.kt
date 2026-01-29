package app.lusk.underseerr.di

import app.lusk.underseerr.presentation.auth.AuthViewModel
import app.lusk.underseerr.presentation.discovery.DiscoveryViewModel
import app.lusk.underseerr.presentation.issue.IssueViewModel
import app.lusk.underseerr.presentation.main.MainViewModel
import app.lusk.underseerr.presentation.profile.ProfileViewModel
import app.lusk.underseerr.presentation.request.RequestViewModel
import app.lusk.underseerr.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { MainViewModel(get(), get(), get()) }
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { RequestViewModel(get(), get()) }
    viewModel { IssueViewModel(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
}
