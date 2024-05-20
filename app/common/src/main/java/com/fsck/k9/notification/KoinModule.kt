package com.fsck.k9.notification

import org.koin.dsl.module

// Define a Koin module to provide dependencies related to notifications
val notificationModule = module {
    // Provide a singleton instance of NotificationActionCreator
    single<NotificationActionCreator> {
        K9NotificationActionCreator(
            context = get(),
            defaultFolderProvider = get(),
            messageStoreManager = get()
        )
    }
    // Provide a singleton instance of NotificationResourceProvider
    single<NotificationResourceProvider> {
        K9NotificationResourceProvider(get())
    }
    // Provide a singleton instance of NotificationStrategy
    single<NotificationStrategy> {
        K9NotificationStrategy(get())
    }
}

