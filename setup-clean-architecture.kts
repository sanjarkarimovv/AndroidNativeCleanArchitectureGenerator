#!/usr/bin/env kotlin

import java.io.File

// ─── Configuration Data Classes ───

data class FlavorConfig(
    val name: String,
    val appName: String,
    val baseUrl: String,
    val applicationIdSuffix: String? = null,
    val versionNameSuffix: String? = null
)

data class ProjectConfig(
    val name: String,
    val packageName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val compileSdk: Int,
    val flavors: List<FlavorConfig>,
    val useRetrofit: Boolean,
    val useRoom: Boolean,
    val useDataStore: Boolean,
    val useKtlint: Boolean,
    val useDetekt: Boolean
)

// ─── Argument Parser ───

fun parseArguments(args: Array<String>): ProjectConfig? {
    if (args.isEmpty()) return null // interactive mode

    var name = ""
    var packageName = ""
    var minSdk = 24
    var targetSdk = 35
    var compileSdk = 35
    var flavorsRaw = ""
    var useRetrofit = false
    var useRoom = false
    var useDataStore = false
    var useKtlint = false
    var useDetekt = false

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--name" -> { name = args[++i] }
            "--package" -> { packageName = args[++i] }
            "--min-sdk" -> { minSdk = args[++i].toInt() }
            "--target-sdk" -> { targetSdk = args[++i].toInt() }
            "--compile-sdk" -> { compileSdk = args[++i].toInt() }
            "--flavors" -> { flavorsRaw = args[++i] }
            "--retrofit" -> { useRetrofit = true }
            "--room" -> { useRoom = true }
            "--datastore" -> { useDataStore = true }
            "--ktlint" -> { useKtlint = true }
            "--detekt" -> { useDetekt = true }
            "--help", "-h" -> {
                printHelp()
                kotlin.system.exitProcess(0)
            }
        }
        i++
    }

    val flavors = parseFlavors(flavorsRaw)

    require(name.isNotBlank()) { "Project name is required (--name)" }
    require(packageName.isNotBlank()) { "Package name is required (--package)" }
    require(flavors.isNotEmpty()) { "At least one flavor is required (--flavors)" }

    return ProjectConfig(
        name = name,
        packageName = packageName,
        minSdk = minSdk,
        targetSdk = targetSdk,
        compileSdk = compileSdk,
        flavors = flavors,
        useRetrofit = useRetrofit,
        useRoom = useRoom,
        useDataStore = useDataStore,
        useKtlint = useKtlint,
        useDetekt = useDetekt
    )
}

fun parseFlavors(raw: String): List<FlavorConfig> {
    if (raw.isBlank()) return emptyList()
    return raw.split(",").map { entry ->
        val parts = entry.split(":", limit = 3)
        require(parts.size >= 3) { "Flavor format: name:appName:baseUrl" }
        val name = parts[0]
        val isProduction = name == "production"
        FlavorConfig(
            name = name,
            appName = parts[1],
            baseUrl = parts[2],
            applicationIdSuffix = if (isProduction) null else ".${name.take(3)}",
            versionNameSuffix = if (isProduction) null else "-$name"
        )
    }
}

fun printHelp() {
    println("""
        |Usage: kotlinc -script setup-clean-architecture.kts [options]
        |
        |Options:
        |  --name <name>           Project name (required in flag mode)
        |  --package <package>     Package name (required in flag mode)
        |  --min-sdk <sdk>         Min SDK version (default: 24)
        |  --target-sdk <sdk>      Target SDK version (default: 35)
        |  --compile-sdk <sdk>     Compile SDK version (default: 35)
        |  --flavors <flavors>     Flavors: "name:appName:url,name:appName:url"
        |  --retrofit              Include Retrofit + Kotlin Serialization
        |  --room                  Include Room database
        |  --datastore             Include DataStore preferences
        |  --ktlint                Include KtLint
        |  --detekt                Include Detekt
        |  --help, -h              Show this help
        |
        |Run without arguments for interactive mode.
    """.trimMargin())
}

// ─── Interactive Mode ───

fun readInput(prompt: String, default: String = ""): String {
    val defaultHint = if (default.isNotBlank()) " (default: $default)" else ""
    print("$prompt$defaultHint: ")
    val input = readlnOrNull()?.trim() ?: ""
    return input.ifBlank { default }
}

fun readYesNo(prompt: String, default: Boolean = false): Boolean {
    val defaultHint = if (default) "Y/n" else "y/N"
    print("$prompt ($defaultHint): ")
    val input = readlnOrNull()?.trim()?.lowercase() ?: ""
    return if (input.isBlank()) default else input == "y" || input == "yes"
}

fun interactiveMode(): ProjectConfig {
    println("╔══════════════════════════════════════════════════╗")
    println("║   Clean Architecture Monolith Setup              ║")
    println("╚══════════════════════════════════════════════════╝")
    println()

    val name = readInput("Project name")
    require(name.isNotBlank()) { "Project name cannot be empty" }

    val packageName = readInput("Package name", "com.example.${name.lowercase()}")
    val minSdk = readInput("Min SDK version", "24").toInt()
    val targetSdk = readInput("Target SDK version", "35").toInt()
    val compileSdk = readInput("Compile SDK version", "35").toInt()

    println()
    println("── Product Flavors ──")
    val flavorCount = readInput("How many product flavors?", "2").toInt()

    val defaultFlavorNames = listOf("development", "production")
    val defaultAppNames = listOf("${name}(dev)", name)

    val flavors = (1..flavorCount).map { index ->
        val defaultName = defaultFlavorNames.getOrElse(index - 1) { "flavor$index" }
        val defaultAppName = defaultAppNames.getOrElse(index - 1) { name }

        val flavorName = readInput("Flavor $index name?", defaultName)
        val appName = readInput("Flavor $index app name?", defaultAppName)
        val baseUrl = readInput("Flavor $index base URL?")

        val isProduction = flavorName == "production"
        FlavorConfig(
            name = flavorName,
            appName = appName,
            baseUrl = baseUrl,
            applicationIdSuffix = if (isProduction) null else ".${flavorName.take(3)}",
            versionNameSuffix = if (isProduction) null else "-$flavorName"
        )
    }

    println()
    println("── Optional Components ──")
    val useRetrofit = readYesNo("Include Retrofit + Kotlin Serialization?")
    val useRoom = readYesNo("Include Room database?")
    val useDataStore = readYesNo("Include DataStore preferences?")
    val useKtlint = readYesNo("Include KtLint?")
    val useDetekt = readYesNo("Include Detekt?")

    return ProjectConfig(
        name = name,
        packageName = packageName,
        minSdk = minSdk,
        targetSdk = targetSdk,
        compileSdk = compileSdk,
        flavors = flavors,
        useRetrofit = useRetrofit,
        useRoom = useRoom,
        useDataStore = useDataStore,
        useKtlint = useKtlint,
        useDetekt = useDetekt
    )
}

// ─── File Generation Utilities ───

fun createFile(basePath: String, relativePath: String, content: String) {
    val file = File(basePath, relativePath)
    file.parentFile.mkdirs()
    file.writeText(content)
    println("  Created: $relativePath")
}

fun createDirectory(basePath: String, relativePath: String) {
    val dir = File(basePath, relativePath)
    dir.mkdirs()
}

fun ProjectConfig.packagePath(): String = packageName.replace(".", "/")

fun ProjectConfig.appClassName(): String = name.replace(Regex("[^a-zA-Z0-9]"), "") + "Application"

// ═══════════════════════════════════════════════════════════
// ─── Gradle Generators ───
// ═══════════════════════════════════════════════════════════

fun generateRootBuildGradle(config: ProjectConfig): String = buildString {
    appendLine("plugins {")
    appendLine("    alias(libs.plugins.android.application) apply false")
    appendLine("    alias(libs.plugins.kotlin.android) apply false")
    appendLine("    alias(libs.plugins.kotlin.compose) apply false")
    appendLine("    alias(libs.plugins.hilt) apply false")
    appendLine("    alias(libs.plugins.ksp) apply false")
    appendLine("    alias(libs.plugins.kotlin.serialization) apply false")
    if (config.useKtlint) {
        appendLine("    alias(libs.plugins.ktlint) apply false")
    }
    if (config.useDetekt) {
        appendLine("    alias(libs.plugins.detekt) apply false")
    }
    appendLine("}")
}

fun generateSettingsGradle(config: ProjectConfig): String = buildString {
    appendLine("pluginManagement {")
    appendLine("    repositories {")
    appendLine("        google {")
    appendLine("            content {")
    appendLine("                includeGroupByRegex(\"com\\\\.android.*\")")
    appendLine("                includeGroupByRegex(\"com\\\\.google.*\")")
    appendLine("                includeGroupByRegex(\"androidx.*\")")
    appendLine("            }")
    appendLine("        }")
    appendLine("        mavenCentral()")
    appendLine("        gradlePluginPortal()")
    appendLine("    }")
    appendLine("}")
    appendLine()
    appendLine("dependencyResolutionManagement {")
    appendLine("    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)")
    appendLine("    repositories {")
    appendLine("        google()")
    appendLine("        mavenCentral()")
    appendLine("    }")
    appendLine("}")
    appendLine()
    appendLine("rootProject.name = \"${config.name}\"")
    appendLine("include(\":app\")")
    appendLine()
    appendLine("includeBuild(\"build-logic\")")
}

fun generateGradleProperties(): String = buildString {
    appendLine("org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8")
    appendLine("android.useAndroidX=true")
    appendLine("kotlin.code.style=official")
    appendLine("android.nonTransitiveRClass=true")
    appendLine("org.gradle.parallel=true")
    appendLine("org.gradle.caching=true")
}

fun generateGradleWrapperProperties(): String = buildString {
    appendLine("distributionBase=GRADLE_USER_HOME")
    appendLine("distributionPath=wrapper/dists")
    appendLine("distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip")
    appendLine("networkTimeout=10000")
    appendLine("validateDistributionUrl=true")
    appendLine("zipStoreBase=GRADLE_USER_HOME")
    appendLine("zipStorePath=wrapper/dists")
}

fun generateAppBuildGradle(config: ProjectConfig): String = buildString {
    appendLine("plugins {")
    appendLine("    alias(libs.plugins.android.application)")
    appendLine("    alias(libs.plugins.kotlin.android)")
    appendLine("    alias(libs.plugins.kotlin.compose)")
    appendLine("    alias(libs.plugins.hilt)")
    appendLine("    alias(libs.plugins.ksp)")
    if (config.useRetrofit) {
        appendLine("    alias(libs.plugins.kotlin.serialization)")
    }
    if (config.useKtlint) {
        appendLine("    alias(libs.plugins.ktlint)")
    }
    if (config.useDetekt) {
        appendLine("    alias(libs.plugins.detekt)")
    }
    appendLine("    id(\"kotlin-parcelize\")")
    appendLine("    id(\"${config.packageName}.android.flavors\")")
    appendLine("}")
    appendLine()
    appendLine("kotlin {")
    appendLine("    sourceSets.all {")
    appendLine("        languageSettings.enableLanguageFeature(\"ExplicitBackingFields\")")
    appendLine("    }")
    appendLine("}")
    appendLine()
    appendLine("android {")
    appendLine("    namespace = \"${config.packageName}\"")
    appendLine("    compileSdk = ${config.compileSdk}")
    appendLine()
    appendLine("    defaultConfig {")
    appendLine("        applicationId = \"${config.packageName}\"")
    appendLine("        minSdk = ${config.minSdk}")
    appendLine("        targetSdk = ${config.targetSdk}")
    appendLine("        versionCode = 1")
    appendLine("        versionName = \"1.0.0\"")
    appendLine()
    appendLine("        testInstrumentationRunner = \"androidx.test.runner.AndroidJUnitRunner\"")
    appendLine("    }")
    appendLine()
    appendLine("    buildTypes {")
    appendLine("        release {")
    appendLine("            isMinifyEnabled = true")
    appendLine("            isShrinkResources = true")
    appendLine("            proguardFiles(")
    appendLine("                getDefaultProguardFile(\"proguard-android-optimize.txt\"),")
    appendLine("                \"proguard-rules.pro\"")
    appendLine("            )")
    appendLine("        }")
    appendLine("    }")
    appendLine()
    appendLine("    compileOptions {")
    appendLine("        sourceCompatibility = JavaVersion.VERSION_17")
    appendLine("        targetCompatibility = JavaVersion.VERSION_17")
    appendLine("    }")
    appendLine()
    appendLine("    kotlinOptions {")
    appendLine("        jvmTarget = \"17\"")
    appendLine("    }")
    appendLine()
    appendLine("    buildFeatures {")
    appendLine("        compose = true")
    appendLine("        buildConfig = true")
    appendLine("    }")
    appendLine("}")
    appendLine()
    if (config.useKtlint) {
        appendLine("ktlint {")
        appendLine("    version.set(\"1.7.1\")")
        appendLine("    android.set(true)")
        appendLine("}")
        appendLine()
    }
    if (config.useDetekt) {
        appendLine("detekt {")
        appendLine("    config.setFrom(\"\$projectDir/../detekt.yml\")")
        appendLine("}")
        appendLine()
    }
    appendLine("dependencies {")
    appendLine("    // Core")
    appendLine("    implementation(libs.androidx.core.ktx)")
    appendLine("    implementation(libs.androidx.appcompat)")
    appendLine("    implementation(libs.androidx.lifecycle.viewmodel.ktx)")
    appendLine("    implementation(libs.androidx.lifecycle.runtime.ktx)")
    appendLine()
    appendLine("    // Compose")
    appendLine("    implementation(platform(libs.androidx.compose.bom))")
    appendLine("    implementation(libs.androidx.compose.ui)")
    appendLine("    implementation(libs.androidx.compose.ui.graphics)")
    appendLine("    implementation(libs.androidx.compose.ui.tooling.preview)")
    appendLine("    implementation(libs.androidx.compose.material3)")
    appendLine("    implementation(libs.androidx.activity.compose)")
    appendLine("    debugImplementation(libs.androidx.compose.ui.tooling)")
    appendLine("    debugImplementation(libs.androidx.compose.ui.test.manifest)")
    appendLine()
    appendLine("    // Coroutines")
    appendLine("    implementation(libs.kotlinx.coroutines.core)")
    appendLine("    implementation(libs.kotlinx.coroutines.android)")
    appendLine()
    appendLine("    // Hilt")
    appendLine("    implementation(libs.hilt.android)")
    appendLine("    ksp(libs.hilt.compiler)")
    appendLine()
    if (config.useRetrofit) {
        appendLine("    // Networking")
        appendLine("    implementation(libs.retrofit)")
        appendLine("    implementation(libs.okhttp)")
        appendLine("    implementation(libs.okhttp.logging)")
        appendLine("    implementation(libs.retrofit.kotlinx.serialization)")
        appendLine("    implementation(libs.kotlinx.serialization.json)")
        appendLine()
    }
    if (config.useRoom) {
        appendLine("    // Room")
        appendLine("    implementation(libs.room.runtime)")
        appendLine("    implementation(libs.room.ktx)")
        appendLine("    ksp(libs.room.compiler)")
        appendLine()
    }
    if (config.useDataStore) {
        appendLine("    // DataStore")
        appendLine("    implementation(libs.datastore.preferences)")
        appendLine()
    }
    appendLine("    // Unit Testing")
    appendLine("    testImplementation(libs.junit)")
    appendLine("    testImplementation(libs.mockito.kotlin)")
    appendLine("    testImplementation(libs.kotlinx.coroutines.test)")
    appendLine("    testImplementation(libs.turbine)")
    appendLine()
    appendLine("    // Android Testing")
    appendLine("    androidTestImplementation(libs.androidx.junit)")
    appendLine("    androidTestImplementation(libs.androidx.espresso.core)")
    appendLine("    androidTestImplementation(platform(libs.androidx.compose.bom))")
    appendLine("    androidTestImplementation(libs.androidx.compose.ui.test.junit4)")
    appendLine("    androidTestImplementation(libs.androidx.uiautomator)")
    appendLine("}")
}

fun generateVersionCatalog(config: ProjectConfig): String = buildString {
    appendLine("[versions]")
    appendLine("agp = \"8.7.3\"")
    appendLine("kotlin = \"2.1.0\"")
    appendLine("ksp = \"2.1.0-1.0.29\"")
    appendLine("coreKtx = \"1.15.0\"")
    appendLine("appcompat = \"1.7.0\"")
    appendLine("lifecycleKtx = \"2.8.7\"")
    appendLine("activityCompose = \"1.9.3\"")
    appendLine("composeBom = \"2024.12.01\"")
    appendLine("coroutines = \"1.9.0\"")
    appendLine("hilt = \"2.54\"")
    if (config.useRetrofit) {
        appendLine("retrofit = \"2.11.0\"")
        appendLine("okhttp = \"4.12.0\"")
        appendLine("kotlinxSerializationJson = \"1.7.3\"")
        appendLine("retrofitKotlinxSerialization = \"1.0.0\"")
    }
    if (config.useRoom) {
        appendLine("room = \"2.6.1\"")
    }
    if (config.useDataStore) {
        appendLine("datastore = \"1.1.1\"")
    }
    appendLine("junit = \"4.13.2\"")
    appendLine("mockitoKotlin = \"5.4.0\"")
    appendLine("turbine = \"1.2.0\"")
    appendLine("junitAndroidx = \"1.2.1\"")
    appendLine("espresso = \"3.6.1\"")
    appendLine("uiautomator = \"2.3.0\"")
    if (config.useKtlint) {
        appendLine("ktlint = \"12.1.2\"")
    }
    if (config.useDetekt) {
        appendLine("detekt = \"1.23.7\"")
    }
    appendLine()
    appendLine("[libraries]")
    appendLine("androidx-core-ktx = { group = \"androidx.core\", name = \"core-ktx\", version.ref = \"coreKtx\" }")
    appendLine("androidx-appcompat = { group = \"androidx.appcompat\", name = \"appcompat\", version.ref = \"appcompat\" }")
    appendLine("androidx-lifecycle-viewmodel-ktx = { group = \"androidx.lifecycle\", name = \"lifecycle-viewmodel-ktx\", version.ref = \"lifecycleKtx\" }")
    appendLine("androidx-lifecycle-runtime-ktx = { group = \"androidx.lifecycle\", name = \"lifecycle-runtime-ktx\", version.ref = \"lifecycleKtx\" }")
    appendLine("androidx-activity-compose = { group = \"androidx.activity\", name = \"activity-compose\", version.ref = \"activityCompose\" }")
    appendLine("androidx-compose-bom = { group = \"androidx.compose\", name = \"compose-bom\", version.ref = \"composeBom\" }")
    appendLine("androidx-compose-ui = { group = \"androidx.compose.ui\", name = \"ui\" }")
    appendLine("androidx-compose-ui-graphics = { group = \"androidx.compose.ui\", name = \"ui-graphics\" }")
    appendLine("androidx-compose-ui-tooling = { group = \"androidx.compose.ui\", name = \"ui-tooling\" }")
    appendLine("androidx-compose-ui-tooling-preview = { group = \"androidx.compose.ui\", name = \"ui-tooling-preview\" }")
    appendLine("androidx-compose-ui-test-manifest = { group = \"androidx.compose.ui\", name = \"ui-test-manifest\" }")
    appendLine("androidx-compose-ui-test-junit4 = { group = \"androidx.compose.ui\", name = \"ui-test-junit4\" }")
    appendLine("androidx-compose-material3 = { group = \"androidx.compose.material3\", name = \"material3\" }")
    appendLine("kotlinx-coroutines-core = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-coroutines-core\", version.ref = \"coroutines\" }")
    appendLine("kotlinx-coroutines-android = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-coroutines-android\", version.ref = \"coroutines\" }")
    appendLine("hilt-android = { group = \"com.google.dagger\", name = \"hilt-android\", version.ref = \"hilt\" }")
    appendLine("hilt-compiler = { group = \"com.google.dagger\", name = \"hilt-android-compiler\", version.ref = \"hilt\" }")
    if (config.useRetrofit) {
        appendLine("retrofit = { group = \"com.squareup.retrofit2\", name = \"retrofit\", version.ref = \"retrofit\" }")
        appendLine("okhttp = { group = \"com.squareup.okhttp3\", name = \"okhttp\", version.ref = \"okhttp\" }")
        appendLine("okhttp-logging = { group = \"com.squareup.okhttp3\", name = \"logging-interceptor\", version.ref = \"okhttp\" }")
        appendLine("retrofit-kotlinx-serialization = { group = \"com.jakewharton.retrofit\", name = \"retrofit2-kotlinx-serialization-converter\", version.ref = \"retrofitKotlinxSerialization\" }")
        appendLine("kotlinx-serialization-json = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-serialization-json\", version.ref = \"kotlinxSerializationJson\" }")
    }
    if (config.useRoom) {
        appendLine("room-runtime = { group = \"androidx.room\", name = \"room-runtime\", version.ref = \"room\" }")
        appendLine("room-ktx = { group = \"androidx.room\", name = \"room-ktx\", version.ref = \"room\" }")
        appendLine("room-compiler = { group = \"androidx.room\", name = \"room-compiler\", version.ref = \"room\" }")
    }
    if (config.useDataStore) {
        appendLine("datastore-preferences = { group = \"androidx.datastore\", name = \"datastore-preferences\", version.ref = \"datastore\" }")
    }
    appendLine("# Build Logic")
    appendLine("android-gradlePlugin = { group = \"com.android.tools.build\", name = \"gradle\", version.ref = \"agp\" }")
    appendLine("kotlin-gradlePlugin = { group = \"org.jetbrains.kotlin\", name = \"kotlin-gradle-plugin\", version.ref = \"kotlin\" }")
    appendLine("compose-gradlePlugin = { group = \"org.jetbrains.kotlin\", name = \"compose-compiler-gradle-plugin\", version.ref = \"kotlin\" }")
    appendLine()
    appendLine("# Testing")
    appendLine("junit = { group = \"junit\", name = \"junit\", version.ref = \"junit\" }")
    appendLine("mockito-kotlin = { group = \"org.mockito.kotlin\", name = \"mockito-kotlin\", version.ref = \"mockitoKotlin\" }")
    appendLine("kotlinx-coroutines-test = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-coroutines-test\", version.ref = \"coroutines\" }")
    appendLine("turbine = { group = \"app.cash.turbine\", name = \"turbine\", version.ref = \"turbine\" }")
    appendLine("androidx-junit = { group = \"androidx.test.ext\", name = \"junit\", version.ref = \"junitAndroidx\" }")
    appendLine("androidx-espresso-core = { group = \"androidx.test.espresso\", name = \"espresso-core\", version.ref = \"espresso\" }")
    appendLine("androidx-uiautomator = { group = \"androidx.test.uiautomator\", name = \"uiautomator\", version.ref = \"uiautomator\" }")
    appendLine()
    appendLine("[plugins]")
    appendLine("android-application = { id = \"com.android.application\", version.ref = \"agp\" }")
    appendLine("kotlin-android = { id = \"org.jetbrains.kotlin.android\", version.ref = \"kotlin\" }")
    appendLine("kotlin-compose = { id = \"org.jetbrains.kotlin.plugin.compose\", version.ref = \"kotlin\" }")
    appendLine("hilt = { id = \"com.google.dagger.hilt.android\", version.ref = \"hilt\" }")
    appendLine("ksp = { id = \"com.google.devtools.ksp\", version.ref = \"ksp\" }")
    appendLine("kotlin-serialization = { id = \"org.jetbrains.kotlin.plugin.serialization\", version.ref = \"kotlin\" }")
    if (config.useKtlint) {
        appendLine("ktlint = { id = \"org.jlleitschuh.gradle.ktlint\", version.ref = \"ktlint\" }")
    }
    if (config.useDetekt) {
        appendLine("detekt = { id = \"io.gitlab.arturbosch.detekt\", version.ref = \"detekt\" }")
    }
}

// ═══════════════════════════════════════════════════════════
// ─── Architecture Domain Templates ───
// ═══════════════════════════════════════════════════════════

fun generateUseCase(packageName: String): String = """
package $packageName.architecture.domain.usecase

interface UseCase<REQUEST, RESULT> {
    fun execute(input: REQUEST, onResult: (RESULT) -> Unit)
}
""".trimIndent()

fun generateBackgroundExecutingUseCase(packageName: String): String = """
package $packageName.architecture.domain.usecase

import $packageName.coroutine.CoroutineContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BackgroundExecutingUseCase<REQUEST, RESULT>(
    private val coroutineContextProvider: CoroutineContextProvider,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : UseCase<REQUEST, RESULT> {
    final override fun execute(input: REQUEST, onResult: (RESULT) -> Unit) {
        coroutineScope.launch {
            val result = withContext(coroutineContextProvider.io) {
                executeInBackground(input)
            }
            onResult(result)
        }
    }

    abstract fun executeInBackground(request: REQUEST): RESULT
}
""".trimIndent()

fun generateContinuousExecutingUseCase(packageName: String): String = """
package $packageName.architecture.domain.usecase

import $packageName.coroutine.CoroutineContextProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class ContinuousExecutingUseCase<REQUEST, RESULT>(
    private val coroutineContextProvider: CoroutineContextProvider,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : UseCase<REQUEST, RESULT> {
    final override fun execute(input: REQUEST, onResult: (RESULT) -> Unit) {
        try {
            coroutineScope.launch {
                withContext(coroutineContextProvider.io) {
                    executeInBackground(input).collect { result ->
                        withContext(coroutineContextProvider.main) {
                            onResult(result)
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
        }
    }

    abstract fun executeInBackground(request: REQUEST): Flow<RESULT>
}
""".trimIndent()

fun generateUseCaseExecutor(packageName: String): String = """
package $packageName.architecture.domain

import $packageName.architecture.domain.exception.DomainException
import $packageName.architecture.domain.exception.UnknownDomainException
import $packageName.architecture.domain.usecase.UseCase

class UseCaseExecutor {
    fun <OUTPUT> execute(
        useCase: UseCase<Unit, OUTPUT>,
        onResult: (OUTPUT) -> Unit = {},
        onException: (DomainException) -> Unit = {}
    ) = execute(useCase, Unit, onResult, onException)

    fun <INPUT, OUTPUT> execute(
        useCase: UseCase<INPUT, OUTPUT>,
        value: INPUT,
        onResult: (OUTPUT) -> Unit = {},
        onException: (DomainException) -> Unit = {}
    ) {
        try {
            useCase.execute(value, onResult)
        } catch (@Suppress("TooGenericExceptionCaught") throwable: Throwable) {
            val domainException =
                ((throwable as? DomainException) ?: UnknownDomainException(throwable))
            onException(domainException)
        }
    }
}
""".trimIndent()

fun generateUseCaseExecutorProvider(packageName: String): String = """
package $packageName.architecture.domain

import kotlinx.coroutines.CoroutineScope

typealias UseCaseExecutorProvider =
    @JvmSuppressWildcards (coroutineScope: CoroutineScope) -> UseCaseExecutor
""".trimIndent()

fun generateDomainException(packageName: String): String = """
package $packageName.architecture.domain.exception

abstract class DomainException(cause: Throwable? = null) : Exception(cause)
""".trimIndent()

fun generateUnknownDomainException(packageName: String): String = """
package $packageName.architecture.domain.exception

class UnknownDomainException(cause: Throwable? = null) : DomainException(cause)
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Architecture Presentation Templates ───
// ═══════════════════════════════════════════════════════════

fun generateBaseViewModel(packageName: String): String = """
package $packageName.architecture.presentation.viewmodel

import $packageName.architecture.domain.UseCaseExecutor
import $packageName.architecture.domain.exception.DomainException
import $packageName.architecture.domain.usecase.UseCase
import $packageName.architecture.presentation.navigation.PresentationNavigationEvent
import $packageName.architecture.presentation.notification.PresentationNotification
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<VIEW_STATE : Any, NOTIFICATION : PresentationNotification>(
    private val useCaseExecutor: UseCaseExecutor
) {
    val viewState: Flow<VIEW_STATE>
        field = MutableSharedFlow()

    val notification: Flow<NOTIFICATION>
        field = MutableSharedFlow()

    val navigationEvent: Flow<PresentationNavigationEvent>
        field = MutableSharedFlow()

    protected fun updateViewState(newState: VIEW_STATE) {
        MainScope().launch {
            viewState.emit(newState)
        }
    }

    protected fun notify(notification: NOTIFICATION) {
        MainScope().launch {
            this@BaseViewModel.notification.emit(notification)
        }
    }

    protected fun emitNavigationEvent(navigationEvent: PresentationNavigationEvent) {
        MainScope().launch {
            this@BaseViewModel.navigationEvent.emit(navigationEvent)
        }
    }

    protected operator fun <OUTPUT> UseCase<Unit, OUTPUT>.invoke(
        onResult: (OUTPUT) -> Unit = {},
        onException: (DomainException) -> Unit = {}
    ) {
        useCaseExecutor.execute(this, onResult, onException)
    }

    protected operator fun <INPUT, OUTPUT> UseCase<INPUT, OUTPUT>.invoke(
        value: INPUT,
        onResult: (OUTPUT) -> Unit = {},
        onException: (DomainException) -> Unit = {}
    ) {
        useCaseExecutor.execute(this, value, onResult, onException)
    }
}
""".trimIndent()

fun generatePresentationNavigationEvent(packageName: String): String = """
package $packageName.architecture.presentation.navigation

interface PresentationNavigationEvent {
    object Back : PresentationNavigationEvent
}
""".trimIndent()

fun generatePresentationNotification(packageName: String): String = """
package $packageName.architecture.presentation.notification

interface PresentationNotification
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Architecture UI Templates ───
// ═══════════════════════════════════════════════════════════

fun generateNavigationEventDestinationMapper(packageName: String): String = """
package $packageName.architecture.ui.navigation.mapper

import $packageName.architecture.presentation.navigation.PresentationNavigationEvent
import $packageName.architecture.ui.navigation.exception.UnhandledNavigationException
import $packageName.architecture.ui.navigation.model.UiDestination
import kotlin.reflect.KClass

abstract class NavigationEventDestinationMapper<in EVENT : PresentationNavigationEvent>(
    private val kotlinClass: KClass<EVENT>
) {
    fun toUi(navigationEvent: PresentationNavigationEvent): UiDestination = when {
        kotlinClass.isInstance(navigationEvent) -> {
            @Suppress("UNCHECKED_CAST")
            mapTypedEvent(navigationEvent as EVENT)
        }
        else -> {
            mapGenericEvent(navigationEvent) ?: throw UnhandledNavigationException(navigationEvent)
        }
    }

    protected abstract fun mapTypedEvent(navigationEvent: EVENT): UiDestination

    protected open fun mapGenericEvent(
        navigationEvent: PresentationNavigationEvent
    ): UiDestination? = null
}
""".trimIndent()

fun generateUiDestination(packageName: String): String = """
package $packageName.architecture.ui.navigation.model

fun interface UiDestination {
    fun navigate(backStack: MutableList<Any>)
}
""".trimIndent()

fun generateUnhandledNavigationException(packageName: String): String = """
package $packageName.architecture.ui.navigation.exception

import $packageName.architecture.presentation.navigation.PresentationNavigationEvent

class UnhandledNavigationException(event: PresentationNavigationEvent) :
    IllegalArgumentException(
        "Navigation event ${'$'}{event::class.simpleName} was not handled."
    )
""".trimIndent()

fun generateNotificationUiMapper(packageName: String): String = """
package $packageName.architecture.ui.notification.mapper

import $packageName.architecture.presentation.notification.PresentationNotification
import $packageName.architecture.ui.notification.model.UiNotification

interface NotificationUiMapper<in PRESENTATION_NOTIFICATION : PresentationNotification> {
    fun toUi(notification: PRESENTATION_NOTIFICATION): UiNotification
}
""".trimIndent()

fun generateUiNotification(packageName: String): String = """
package $packageName.architecture.ui.notification.model

fun interface UiNotification {
    fun present()
}
""".trimIndent()

fun generateBaseComposeHolder(packageName: String): String = """
package $packageName.architecture.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import $packageName.architecture.presentation.navigation.PresentationNavigationEvent
import $packageName.architecture.presentation.notification.PresentationNotification
import $packageName.architecture.presentation.viewmodel.BaseViewModel
import $packageName.architecture.ui.navigation.mapper.NavigationEventDestinationMapper
import $packageName.architecture.ui.notification.mapper.NotificationUiMapper

abstract class BaseComposeHolder<VIEW_STATE : Any, NOTIFICATION : PresentationNotification>(
    private val viewModel: BaseViewModel<VIEW_STATE, NOTIFICATION>,
    private val navigationMapper: NavigationEventDestinationMapper<*>,
    private val notificationMapper: NotificationUiMapper<NOTIFICATION>
) {
    @Composable
    fun ViewModelObserver(backStack: MutableList<Any>) {
        viewModel.notification.collectAsState(initial = null)
            .value?.let { notificationValue ->
                Notifier(notification = notificationValue)
            }

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collect { navigationValue ->
                navigate(navigationValue, backStack)
            }
        }
    }

    @Composable
    private fun Notifier(notification: NOTIFICATION) {
        LaunchedEffect(notification) {
            notificationMapper.toUi(notification).present()
        }
    }

    private fun navigate(navigation: PresentationNavigationEvent, backStack: MutableList<Any>) {
        navigationMapper.toUi(navigation).navigate(backStack)
    }
}
""".trimIndent()

fun generateScreenEnterObserver(packageName: String): String = """
package $packageName.architecture.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun ScreenEnterObserver(onEntered: () -> Unit) {
    var entered by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entered) {
        if (!entered) {
            entered = true
            onEntered()
        }
    }
}
""".trimIndent()

fun generateViewsProvider(packageName: String): String = """
package $packageName.architecture.ui.view

interface ViewsProvider
""".trimIndent()

fun generateViewStateBinder(packageName: String): String = """
package $packageName.architecture.ui.binder

import $packageName.architecture.ui.view.ViewsProvider

interface ViewStateBinder<in VIEW_STATE : Any, in VIEWS_PROVIDER : ViewsProvider> {
    fun VIEWS_PROVIDER.bindState(viewState: VIEW_STATE)
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Coroutine Template ───
// ═══════════════════════════════════════════════════════════

fun generateCoroutineContextProvider(packageName: String): String = """
package $packageName.coroutine

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

interface CoroutineContextProvider {
    val main: CoroutineContext
    val io: CoroutineContext

    object Default : CoroutineContextProvider {
        override val main: CoroutineContext = Dispatchers.Main
        override val io: CoroutineContext = Dispatchers.IO
    }
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── DataSource Templates ───
// ═══════════════════════════════════════════════════════════

fun generateDataException(packageName: String): String = """
package $packageName.datasource.architecture.exception

abstract class DataException(cause: Throwable? = null) : Exception(cause)
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Analytics Templates ───
// ═══════════════════════════════════════════════════════════

fun generateAnalytics(packageName: String): String = """
package $packageName.analytics

interface Analytics {
    fun logScreen(screenName: String)
    fun logEvent(event: AnalyticsEvent)
}
""".trimIndent()

fun generateAnalyticsEvent(packageName: String): String = """
package $packageName.analytics

abstract class AnalyticsEvent(val eventName: String, open val eventProperties: Map<String, Any>)
""".trimIndent()

fun generateBogusAnalytics(packageName: String): String = """
package $packageName.analytics.bogus

import $packageName.analytics.Analytics
import $packageName.analytics.AnalyticsEvent

private var lastScreenName: String? = null

class BogusAnalytics : Analytics {
    override fun logScreen(screenName: String) {
        lastScreenName = screenName
        println("Event recorded: Entered ${'$'}screenName")
    }

    override fun logEvent(event: AnalyticsEvent) {
        println("Event recorded: ${'$'}lastScreenName -> ${'$'}{event.eventName}")
        event.eventProperties.forEach { (eventKey, eventValue) ->
            println("${'$'}eventKey: ${'$'}eventValue")
        }
    }
}
""".trimIndent()

fun generateClickEvent(packageName: String): String = """
package $packageName.analytics.event

import $packageName.analytics.AnalyticsEvent

data class Click(
    private val buttonName: String,
    override val eventProperties: Map<String, Any> = emptyMap()
) : AnalyticsEvent(
    eventName = "Click: ${'$'}buttonName",
    eventProperties = eventProperties
)
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Test Templates ───
// ═══════════════════════════════════════════════════════════

fun generateFakeCoroutineContext(packageName: String): String = """
package $packageName.coroutine

import kotlin.coroutines.CoroutineContext

private class FakeCoroutineContext : CoroutineContext {
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R = initial
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = null
    override fun minusKey(key: CoroutineContext.Key<*>) = this
}

val fakeCoroutineContext: CoroutineContext = FakeCoroutineContext()
""".trimIndent()

fun generateFakeCoroutineContextProvider(packageName: String): String = """
package $packageName.coroutine

import kotlin.coroutines.CoroutineContext

private class FakeCoroutineContextProvider(
    override val main: CoroutineContext = fakeCoroutineContext,
    override val io: CoroutineContext = fakeCoroutineContext
) : CoroutineContextProvider

val fakeCoroutineContextProvider: CoroutineContextProvider =
    FakeCoroutineContextProvider()
""".trimIndent()

fun generateFlowCurrentValueReader(packageName: String): String = """
package $packageName.coroutine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

suspend fun <VALUE_TYPE> Flow<VALUE_TYPE>.currentValue() = take(1).toList().first()
""".trimIndent()

fun generateBaseViewModelTest(packageName: String): String = """
package $packageName.architecture.presentation.viewmodel

import $packageName.architecture.domain.UseCaseExecutor
import $packageName.architecture.domain.exception.DomainException
import $packageName.architecture.domain.usecase.UseCase
import $packageName.architecture.presentation.notification.PresentationNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mock
import org.mockito.stubbing.Answer

private const val NO_INPUT_ON_RESULT_ARGUMENT_INDEX = 1
private const val NO_INPUT_ON_EXCEPTION_ARGUMENT_INDEX = 2
private const val ON_RESULT_ARGUMENT_INDEX = 2
private const val ON_EXCEPTION_ARGUMENT_INDEX = 3

abstract class BaseViewModelTest<
    VIEW_STATE : Any,
    NOTIFICATION : PresentationNotification,
    VIEW_MODEL : BaseViewModel<VIEW_STATE, NOTIFICATION>
> {
    private val testScheduler = TestCoroutineScheduler()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    protected lateinit var classUnderTest: VIEW_MODEL

    @Mock
    protected lateinit var useCaseExecutor: UseCaseExecutor

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun coroutineSetUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun coroutineTearDown() {
        Dispatchers.resetMain()
    }

    protected fun UseCase<Unit, *>.givenFailedExecution(domainException: DomainException) {
        givenExecutionWillAnswer { invocation ->
            val onException: (DomainException) -> Unit =
                invocation.getArgument(NO_INPUT_ON_EXCEPTION_ARGUMENT_INDEX)
            onException(domainException)
        }
    }

    protected fun <REQUEST> UseCase<REQUEST, *>.givenFailedExecution(
        input: REQUEST,
        domainException: DomainException
    ) {
        givenExecutionWillAnswer(input) { invocation ->
            val onException: (DomainException) -> Unit =
                invocation.getArgument(ON_EXCEPTION_ARGUMENT_INDEX)
            onException(domainException)
        }
    }

    protected fun <REQUEST, RESULT> UseCase<REQUEST, RESULT>.givenSuccessfulExecution(
        input: REQUEST,
        result: RESULT
    ) {
        givenExecutionWillAnswer(input) { invocation ->
            val onResult: (RESULT) -> Unit = invocation.getArgument(ON_RESULT_ARGUMENT_INDEX)
            onResult(result)
        }
    }

    protected inline fun <reified REQUEST> UseCase<REQUEST, Unit>.givenSuccessfulNoResultExecution(
        input: REQUEST
    ) {
        givenSuccessfulExecution(input, Unit)
    }

    protected fun <RESULT> UseCase<Unit, RESULT>.givenSuccessfulExecution(result: RESULT) {
        givenExecutionWillAnswer { invocationOnMock ->
            val onResult: (RESULT) -> Unit =
                invocationOnMock.getArgument(NO_INPUT_ON_RESULT_ARGUMENT_INDEX)
            onResult(result)
        }
    }

    protected fun UseCase<Unit, Unit>.givenSuccessfulNoArgumentNoResultExecution() {
        givenExecutionWillAnswer { invocationOnMock ->
            val onResult: (Unit) -> Unit = invocationOnMock.getArgument(ON_RESULT_ARGUMENT_INDEX)
            onResult(Unit)
        }
    }

    private fun <RESULT> UseCase<Unit, RESULT>.givenExecutionWillAnswer(answer: Answer<*>) {
        willAnswer(answer).given(useCaseExecutor).execute(
            useCase = eq(this@givenExecutionWillAnswer) ?: this@givenExecutionWillAnswer,
            onResult = any() ?: {},
            onException = any() ?: {}
        )
    }

    private fun <REQUEST, RESULT> UseCase<REQUEST, RESULT>.givenExecutionWillAnswer(
        input: REQUEST,
        answer: Answer<*>
    ) {
        willAnswer(answer).given(useCaseExecutor).execute(
            useCase = eq(this@givenExecutionWillAnswer) ?: this@givenExecutionWillAnswer,
            value = eq(input) ?: input,
            onResult = any() ?: {},
            onException = any() ?: {}
        )
    }
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Optional: Retrofit Templates ───
// ═══════════════════════════════════════════════════════════

fun generateNetworkModule(packageName: String): String = """
package $packageName.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import $packageName.BuildConfig
import $packageName.datasource.network.AuthInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
""".trimIndent()

fun generateAuthInterceptor(packageName: String): String = """
package $packageName.datasource.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            // .addHeader("Authorization", "Bearer token")
            .build()
        return chain.proceed(request)
    }
}
""".trimIndent()

fun generateApiErrorHandler(packageName: String): String = """
package $packageName.datasource.network

import $packageName.datasource.architecture.exception.DataException

class ApiException(
    val code: Int,
    override val message: String,
    cause: Throwable? = null
) : DataException(cause)

fun handleApiError(code: Int, message: String): Nothing {
    throw ApiException(code, message)
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Optional: Room Templates ───
// ═══════════════════════════════════════════════════════════

fun generateAppDatabase(packageName: String): String = """
package $packageName.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase()
""".trimIndent()

fun generateDatabaseModule(packageName: String): String = """
package $packageName.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import $packageName.datasource.local.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "app_database"
    ).build()
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Optional: DataStore Templates ───
// ═══════════════════════════════════════════════════════════

fun generatePreferencesManager(packageName: String): String = """
package $packageName.datasource.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class PreferencesManager(private val dataStore: DataStore<Preferences>) {
    fun <T> get(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        dataStore.data.map { preferences -> preferences[key] ?: defaultValue }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences -> preferences[key] = value }
    }

    suspend fun <T> remove(key: Preferences.Key<T>) {
        dataStore.edit { preferences -> preferences.remove(key) }
    }
}
""".trimIndent()

fun generateDataStoreModule(packageName: String): String = """
package $packageName.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import $packageName.datasource.local.PreferencesManager
import $packageName.datasource.local.dataStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context.dataStore)
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── App Entry Point Templates ───
// ═══════════════════════════════════════════════════════════

fun generateApplication(packageName: String, appClassName: String): String = """
package $packageName

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class $appClassName : Application()
""".trimIndent()

fun generateMainActivity(packageName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // TODO: Add navigation host
                }
            }
        }
    }
}
""".trimIndent()

fun generateAndroidManifest(packageName: String, appClassName: String): String = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".$appClassName"
        android:allowBackup="true"
        android:icon="${"$"}{appIcon}"
        android:label="@string/app_name"
        android:roundIcon="${"$"}{appIconRound}"
        android:supportsRtl="true"
        android:theme="@style/Theme.Material3.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Material3.DayNight.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
""".trimIndent()

fun generateAppModule(packageName: String): String = """
package $packageName.di

import $packageName.architecture.domain.UseCaseExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideUseCaseExecutor(): UseCaseExecutor = UseCaseExecutor()
}
""".trimIndent()

fun generateArchitectureModule(packageName: String): String = """
package $packageName.di

import $packageName.coroutine.CoroutineContextProvider
import $packageName.analytics.Analytics
import $packageName.analytics.bogus.BogusAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ArchitectureModule {
    @Provides
    @Singleton
    fun provideCoroutineContextProvider(): CoroutineContextProvider =
        CoroutineContextProvider.Default

    @Provides
    @Singleton
    fun provideAnalytics(): Analytics = BogusAnalytics()
}
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Build Logic Convention Module Templates ───
// ═══════════════════════════════════════════════════════════

fun generateBuildLogicSettingsGradle(config: ProjectConfig): String = buildString {
    appendLine("pluginManagement {")
    appendLine("    repositories {")
    appendLine("        mavenCentral()")
    appendLine("        gradlePluginPortal()")
    appendLine("        google()")
    appendLine("    }")
    appendLine("}")
    appendLine()
    appendLine("dependencyResolutionManagement {")
    appendLine("    repositories {")
    appendLine("        google()")
    appendLine("        mavenCentral()")
    appendLine("    }")
    appendLine("    versionCatalogs {")
    appendLine("        create(\"libs\") {")
    appendLine("            from(files(\"../gradle/libs.versions.toml\"))")
    appendLine("        }")
    appendLine("    }")
    appendLine("}")
    appendLine()
    appendLine("rootProject.name = \"build-logic\"")
    appendLine("include(\":convention\")")
}

fun generateBuildLogicGradleProperties(): String = buildString {
    appendLine("# Gradle properties are not passed to included builds https://github.com/gradle/gradle/issues/2534")
    appendLine("org.gradle.parallel=true")
    appendLine("org.gradle.caching=true")
    appendLine("org.gradle.configureondemand=true")
}

fun generateConventionBuildGradle(config: ProjectConfig): String = buildString {
    appendLine("plugins {")
    appendLine("    `kotlin-dsl`")
    appendLine("}")
    appendLine()
    appendLine("group = \"${config.packageName}.build-logic\"")
    appendLine()
    appendLine("java {")
    appendLine("    sourceCompatibility = JavaVersion.VERSION_17")
    appendLine("    targetCompatibility = JavaVersion.VERSION_17")
    appendLine("}")
    appendLine()
    appendLine("kotlin {")
    appendLine("    jvmToolchain(17)")
    appendLine("}")
    appendLine()
    appendLine("dependencies {")
    appendLine("    compileOnly(libs.android.gradlePlugin)")
    appendLine("    compileOnly(libs.kotlin.gradlePlugin)")
    appendLine("    compileOnly(libs.compose.gradlePlugin)")
    appendLine("}")
    appendLine()
    appendLine("gradlePlugin {")
    appendLine("    plugins {")
    appendLine("        register(\"androidFlavors\") {")
    appendLine("            id = \"${config.packageName}.android.flavors\"")
    appendLine("            implementationClass = \"AndroidAppFlavorsConventionPlugin\"")
    appendLine("        }")
    appendLine("    }")
    appendLine("}")
}

fun generateAndroidAppFlavorsConventionPlugin(config: ProjectConfig): String = """
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import ${config.packageName}.convention.configureFlavors

class AndroidAppFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                configureFlavors(this)
            }
        }
    }
}
""".trimIndent()

fun generateAppFlavor(config: ProjectConfig): String = buildString {
    appendLine("package ${config.packageName}.convention")
    appendLine()
    appendLine("import com.android.build.api.dsl.ApplicationExtension")
    appendLine("import com.android.build.api.dsl.ApplicationProductFlavor")
    appendLine("import com.android.build.api.dsl.CommonExtension")
    appendLine("import com.android.build.api.dsl.ProductFlavor")
    appendLine()
    appendLine("enum class FlavorDimension {")
    appendLine("    Environment")
    appendLine("}")
    appendLine()
    appendLine("@Suppress(\"EnumEntryName\")")
    appendLine("enum class AppFlavor(")
    appendLine("    val dimension: FlavorDimension,")
    appendLine("    val applicationIdSuffix: String? = null,")
    appendLine("    val versionNameSuffix: String? = null,")
    appendLine("    val appName: String,")
    appendLine("    val baseApiUrl: String")
    appendLine(") {")
    config.flavors.forEachIndexed { index, flavor ->
        appendLine("    ${flavor.name}(")
        appendLine("        dimension = FlavorDimension.Environment,")
        if (flavor.applicationIdSuffix != null) {
            appendLine("        applicationIdSuffix = \"${flavor.applicationIdSuffix}\",")
        }
        if (flavor.versionNameSuffix != null) {
            appendLine("        versionNameSuffix = \"${flavor.versionNameSuffix}\",")
        }
        appendLine("        appName = \"${flavor.appName}\",")
        appendLine("        baseApiUrl = \"${flavor.baseUrl}\"")
        val separator = if (index < config.flavors.size - 1) "," else ""
        appendLine("    )$separator")
    }
    appendLine("}")
    appendLine()
    appendLine("fun configureFlavors(")
    appendLine("    commonExtension: CommonExtension<*, *, *, *, *, *>,")
    appendLine("    flavorConfigurationBlock: ProductFlavor.(flavor: AppFlavor) -> Unit = {},")
    appendLine(") {")
    appendLine("    commonExtension.apply {")
    appendLine("        flavorDimensions += FlavorDimension.Environment.name")
    appendLine("        productFlavors {")
    appendLine("            AppFlavor.values().forEach {")
    appendLine("                create(it.name) {")
    appendLine("                    dimension = it.dimension.name")
    appendLine("                    flavorConfigurationBlock(this, it)")
    appendLine("                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {")
    appendLine("                        if (it.applicationIdSuffix != null) {")
    appendLine("                            applicationIdSuffix = it.applicationIdSuffix")
    appendLine("                        }")
    appendLine("                        if (it.versionNameSuffix != null) {")
    appendLine("                            versionNameSuffix = it.versionNameSuffix")
    appendLine("                        }")
    appendLine("                    }")
    appendLine("                    resValue(\"string\", \"app_name\", \"\\\"${'$'}{it.appName}\\\"\")")
    appendLine("                    buildConfigField(\"String\", \"BASE_URL\", \"\\\"${'$'}{it.baseApiUrl}\\\"\")")
    appendLine("                    buildConfigField(\"String\", \"LABEL_NAME\", \"\\\"${'$'}{it.name}\\\"\")")
    appendLine("                }")
    appendLine("            }")
    appendLine("        }")
    appendLine("        sourceSets {")
    config.flavors.forEach { flavor ->
        val dirName = when (flavor.name) {
            "development" -> "dev"
            "production" -> "prod"
            else -> flavor.name
        }
        appendLine("            getByName(\"${flavor.name}\") {")
        appendLine("                res.srcDirs(\"src/$dirName/res\")")
        appendLine("            }")
    }
    appendLine("        }")
    appendLine("    }")
    appendLine("}")
}

fun generateProguardRules(): String = """
# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
""".trimIndent()

fun generateEditorConfig(): String = """
root = true

[*]
charset = utf-8
end_of_line = lf
indent_size = 4
indent_style = space
insert_final_newline = true
max_line_length = 120
tab_width = 4
trim_trailing_whitespace = true

[*.{kt,kts}]
ktlint_code_style = android_studio
""".trimIndent()

fun generateDetektConfig(): String = """
build:
  maxIssues: 0

complexity:
  LongMethod:
    threshold: 30
  LongParameterList:
    functionThreshold: 10
    constructorThreshold: 10
  ComplexCondition:
    threshold: 5

style:
  MaxLineLength:
    maxLineLength: 120
  WildcardImport:
    active: true
  ForbiddenComment:
    active: false
""".trimIndent()

// ═══════════════════════════════════════════════════════════
// ─── Main Generator ───
// ═══════════════════════════════════════════════════════════

fun generateProject(config: ProjectConfig) {
    val basePath = config.name
    val pkgPath = config.packagePath()
    val appClassName = config.appClassName()
    val mainSrc = "app/src/main/java/$pkgPath"
    val testSrc = "app/src/test/java/$pkgPath"
    val androidTestSrc = "app/src/androidTest/java/$pkgPath"

    println()
    println("Generating project: ${config.name}")
    println("Package: ${config.packageName}")
    println("=======================================")

    // ── Gradle Files ──
    println("\n-- Gradle Files --")
    createFile(basePath, "build.gradle.kts", generateRootBuildGradle(config))
    createFile(basePath, "settings.gradle.kts", generateSettingsGradle(config))
    createFile(basePath, "gradle.properties", generateGradleProperties())
    createFile(basePath, "gradle/wrapper/gradle-wrapper.properties", generateGradleWrapperProperties())
    createFile(basePath, "app/build.gradle.kts", generateAppBuildGradle(config))
    createFile(basePath, "gradle/libs.versions.toml", generateVersionCatalog(config))

    // ── App Entry Points ──
    println("\n-- App Entry Points --")
    createFile(basePath, "$mainSrc/${appClassName}.kt", generateApplication(config.packageName, appClassName))
    createFile(basePath, "$mainSrc/MainActivity.kt", generateMainActivity(config.packageName))
    createFile(basePath, "app/src/main/AndroidManifest.xml", generateAndroidManifest(config.packageName, appClassName))

    // ── Architecture Domain ──
    println("\n-- Architecture Domain --")
    createFile(basePath, "$mainSrc/architecture/domain/usecase/UseCase.kt", generateUseCase(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/usecase/BackgroundExecutingUseCase.kt", generateBackgroundExecutingUseCase(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/usecase/ContinuousExecutingUseCase.kt", generateContinuousExecutingUseCase(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/UseCaseExecutor.kt", generateUseCaseExecutor(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/UseCaseExecutorProvider.kt", generateUseCaseExecutorProvider(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/exception/DomainException.kt", generateDomainException(config.packageName))
    createFile(basePath, "$mainSrc/architecture/domain/exception/UnknownDomainException.kt", generateUnknownDomainException(config.packageName))

    // ── Architecture Presentation ──
    println("\n-- Architecture Presentation --")
    createFile(basePath, "$mainSrc/architecture/presentation/viewmodel/BaseViewModel.kt", generateBaseViewModel(config.packageName))
    createFile(basePath, "$mainSrc/architecture/presentation/navigation/PresentationNavigationEvent.kt", generatePresentationNavigationEvent(config.packageName))
    createFile(basePath, "$mainSrc/architecture/presentation/notification/PresentationNotification.kt", generatePresentationNotification(config.packageName))

    // ── Architecture UI ──
    println("\n-- Architecture UI --")
    createFile(basePath, "$mainSrc/architecture/ui/navigation/mapper/NavigationEventDestinationMapper.kt", generateNavigationEventDestinationMapper(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/navigation/model/UiDestination.kt", generateUiDestination(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/navigation/exception/UnhandledNavigationException.kt", generateUnhandledNavigationException(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/notification/mapper/NotificationUiMapper.kt", generateNotificationUiMapper(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/notification/model/UiNotification.kt", generateUiNotification(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/view/BaseComposeHolder.kt", generateBaseComposeHolder(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/view/ScreenEnterObserver.kt", generateScreenEnterObserver(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/view/ViewsProvider.kt", generateViewsProvider(config.packageName))
    createFile(basePath, "$mainSrc/architecture/ui/binder/ViewStateBinder.kt", generateViewStateBinder(config.packageName))

    // ── Coroutine ──
    println("\n-- Coroutine --")
    createFile(basePath, "$mainSrc/coroutine/CoroutineContextProvider.kt", generateCoroutineContextProvider(config.packageName))

    // ── DataSource ──
    println("\n-- DataSource --")
    createFile(basePath, "$mainSrc/datasource/architecture/exception/DataException.kt", generateDataException(config.packageName))
    createDirectory(basePath, "$mainSrc/datasource/source")

    // ── Analytics ──
    println("\n-- Analytics --")
    createFile(basePath, "$mainSrc/analytics/Analytics.kt", generateAnalytics(config.packageName))
    createFile(basePath, "$mainSrc/analytics/AnalyticsEvent.kt", generateAnalyticsEvent(config.packageName))
    createFile(basePath, "$mainSrc/analytics/bogus/BogusAnalytics.kt", generateBogusAnalytics(config.packageName))
    createFile(basePath, "$mainSrc/analytics/event/Click.kt", generateClickEvent(config.packageName))

    // ── Navigation ──
    println("\n-- Navigation --")
    createDirectory(basePath, "$mainSrc/navigation/mapper")

    // ── Widget ──
    println("\n-- Widget --")
    createDirectory(basePath, "$mainSrc/widget")

    // ── DI ──
    println("\n-- Dependency Injection --")
    createFile(basePath, "$mainSrc/di/AppModule.kt", generateAppModule(config.packageName))
    createFile(basePath, "$mainSrc/di/ArchitectureModule.kt", generateArchitectureModule(config.packageName))

    // ── Build Logic Convention Module ──
    println("\n-- Build Logic Convention --")
    val conventionPkgPath = config.packageName.replace(".", "/")
    createFile(basePath, "build-logic/settings.gradle.kts", generateBuildLogicSettingsGradle(config))
    createFile(basePath, "build-logic/gradle.properties", generateBuildLogicGradleProperties())
    createFile(basePath, "build-logic/convention/build.gradle.kts", generateConventionBuildGradle(config))
    createFile(basePath, "build-logic/convention/src/main/java/AndroidAppFlavorsConventionPlugin.kt", generateAndroidAppFlavorsConventionPlugin(config))
    createFile(basePath, "build-logic/convention/src/main/java/$conventionPkgPath/convention/AppFlavor.kt", generateAppFlavor(config))

    // ── Feature (empty) ──
    println("\n-- Feature Directory --")
    createDirectory(basePath, "$mainSrc/feature")

    // ── Flavor Resource Dirs ──
    println("\n-- Flavor Resources --")
    for (flavor in config.flavors) {
        val dirName = when (flavor.name) {
            "development" -> "dev"
            "production" -> "prod"
            else -> flavor.name
        }
        createDirectory(basePath, "app/src/$dirName/res")
        println("  Created: app/src/$dirName/res/")
    }

    // ── Optional: Retrofit ──
    if (config.useRetrofit) {
        println("\n-- Retrofit --")
        createFile(basePath, "$mainSrc/di/NetworkModule.kt", generateNetworkModule(config.packageName))
        createFile(basePath, "$mainSrc/datasource/network/AuthInterceptor.kt", generateAuthInterceptor(config.packageName))
        createFile(basePath, "$mainSrc/datasource/network/ApiErrorHandler.kt", generateApiErrorHandler(config.packageName))
    }

    // ── Optional: Room ──
    if (config.useRoom) {
        println("\n-- Room --")
        createFile(basePath, "$mainSrc/datasource/local/AppDatabase.kt", generateAppDatabase(config.packageName))
        createFile(basePath, "$mainSrc/di/DatabaseModule.kt", generateDatabaseModule(config.packageName))
    }

    // ── Optional: DataStore ──
    if (config.useDataStore) {
        println("\n-- DataStore --")
        createFile(basePath, "$mainSrc/datasource/local/PreferencesManager.kt", generatePreferencesManager(config.packageName))
        createFile(basePath, "$mainSrc/di/DataStoreModule.kt", generateDataStoreModule(config.packageName))
    }

    // ── Test Infrastructure ──
    println("\n-- Test Infrastructure --")
    createFile(basePath, "$testSrc/coroutine/FakeCoroutineContext.kt", generateFakeCoroutineContext(config.packageName))
    createFile(basePath, "$testSrc/coroutine/FakeCoroutineContextProvider.kt", generateFakeCoroutineContextProvider(config.packageName))
    createFile(basePath, "$testSrc/coroutine/FlowCurrentValueReader.kt", generateFlowCurrentValueReader(config.packageName))
    createFile(basePath, "$testSrc/architecture/presentation/viewmodel/BaseViewModelTest.kt", generateBaseViewModelTest(config.packageName))
    createDirectory(basePath, "$testSrc/feature")

    // ── Android Test Infrastructure ──
    println("\n-- Android Test Infrastructure --")
    createDirectory(basePath, "$androidTestSrc/robot")
    createDirectory(basePath, "$androidTestSrc/feature")

    // ── Config Files ──
    println("\n-- Config Files --")
    createFile(basePath, ".editorconfig", generateEditorConfig())
    createFile(basePath, "app/proguard-rules.pro", generateProguardRules())
    if (config.useDetekt) {
        createFile(basePath, "detekt.yml", generateDetektConfig())
    }

    println()
    println("=======================================")
    println("Project '${config.name}' generated successfully!")
    println()
    println("Next steps:")
    println("  1. cd ${config.name}")
    println("  2. Open in Android Studio")
    println("  3. Sync Gradle")
    println("  4. Run the app")
}

// ═══════════════════════════════════════════════════════════
// ─── Entry Point ───
// ═══════════════════════════════════════════════════════════

val config = parseArguments(args) ?: interactiveMode()

println()
println("Configuration:")
println("  Name: ${config.name}")
println("  Package: ${config.packageName}")
println("  Min SDK: ${config.minSdk}")
println("  Target SDK: ${config.targetSdk}")
println("  Flavors: ${config.flavors.joinToString { it.name }}")
println("  Retrofit: ${config.useRetrofit}")
println("  Room: ${config.useRoom}")
println("  DataStore: ${config.useDataStore}")
println("  KtLint: ${config.useKtlint}")
println("  Detekt: ${config.useDetekt}")

val proceed = readYesNo("\nProceed with generation?", default = true)
if (proceed) {
    generateProject(config)
} else {
    println("Generation cancelled.")
}
