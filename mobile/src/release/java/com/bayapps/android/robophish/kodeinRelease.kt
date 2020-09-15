package com.bayapps.android.robophish

import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val releaseModule = DI.Module("Release Module") {
    bind<AppInitializer>() with singleton { ReleaseAppInitializer() }
}

val buildSpecificModules = listOf(releaseModule)
