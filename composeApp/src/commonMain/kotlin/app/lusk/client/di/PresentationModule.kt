package app.lusk.client.di

import app.lusk.client.presentation.auth.AuthViewModel
import app.lusk.client.presentation.discovery.DiscoveryViewModel
import app.lusk.client.presentation.issue.IssueViewModel
import app.lusk.client.presentation.main.MainViewModel
import app.lusk.client.presentation.profile.ProfileViewModel
import app.lusk.client.presentation.request.RequestViewModel
import app.lusk.client.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { MainViewModel(get()) }
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { RequestViewModel(get()) }
    viewModel { IssueViewModel(get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
