plugins {
    idea
    `java-library`

    kotlin("jvm")
    kotlin("kapt")

    id("api-key-provider")
    id("kotlin-config-writer")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

dependencies {
    implementation(kotlin("stdlib"))

    // api to expose networkingModule.kt to consumers
    api("org.kodein.di:kodein-di:7.6.0")

    // api to expose Interceptors and HttpUrl to consumers
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    api("dev.forkhandles:result4k:1.10.0.0")

    implementation("com.jakewharton.byteunits:byteunits:0.9.1")

    implementation("com.squareup.okio:okio:2.10.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    implementation("com.squareup.moshi:moshi:1.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.12.0")
    implementation("com.squareup.moshi:moshi-adapters:1.12.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.12.0")

    testImplementation(kotlin("test"))
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.0")
    testImplementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

}

kotlinConfigWriter {
    packageName = "nes.networking"

    val phishNetApiKey: String by project
    put("PHISH_NET_API_KEY", phishNetApiKey)

    val phishinApiKey: String by project
    put("PHISH_IN_API_KEY", phishinApiKey)
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

// setup integration tests w/ intellij/android stuido
// doing it from groovy file because the kotlin dsl is not well equipped to work with this.
apply(from = "idea.gradle")

tasks.register<Test>("integrationTest") {
    val integrationTest by sourceSets
    description = "Runs the integration tests."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    mustRunAfter(tasks.named("compileKotlin").get())
    outputs.upToDateWhen { false }

    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
