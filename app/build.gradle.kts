plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tutozz.blespam"
    compileSdk = 34  // Обновил до 34 для совместимости

    // ========== КОНФИГУРАЦИЯ ПОДПИСИ ==========
    signingConfigs {
        create("debug") {
            // Debug ключ - создается автоматически
            // Или можно указать явно, как ниже
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        create("release") {
            // Release ключ - для финальной сборки
            // Значения берутся из переменных окружения или gradle.properties
            storeFile = file(
                if (project.hasProperty("RELEASE_STORE_FILE")) {
                    project.property("RELEASE_STORE_FILE") as String
                } else {
                    "release.keystore"
                }
            )
            storePassword = 
                if (project.hasProperty("RELEASE_STORE_PASSWORD")) {
                    project.property("RELEASE_STORE_PASSWORD") as String
                } else {
                    System.getenv("KEYSTORE_PASSWORD") ?: ""
                }
            keyAlias = 
                if (project.hasProperty("RELEASE_KEY_ALIAS")) {
                    project.property("RELEASE_KEY_ALIAS") as String
                } else {
                    System.getenv("KEY_ALIAS") ?: "key0"
                }
            keyPassword = 
                if (project.hasProperty("RELEASE_KEY_PASSWORD")) {
                    project.property("RELEASE_KEY_PASSWORD") as String
                } else {
                    System.getenv("KEY_PASSWORD") ?: ""
                }
        }
    }

    defaultConfig {
        applicationId = "com.tutozz.blespam"
        minSdk = 26  // Минимум 26 для BLE (было 24)
        targetSdk = 34  // Обновил до актуальной версии
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            isMinifyEnabled = false
            // Используем debug подпись
            signingConfig = signingConfigs.getByName("debug")
            
            // Можно задать кастомное имя файла APK
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false  // Можно включить true для уменьшения размера
            isShrinkResources = false  // Можно включить true для уменьшения размера
            
            // Используем release подпись
            signingConfig = signingConfigs.getByName("release")
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Можно задать кастомное имя файла APK
            setProperty("archivesBaseName", "BLE-Spam-v${versionName}")
        }
        
        // Опционально: создать кастомный build type
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-STAGING"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // ========== КОНФИГУРАЦИЯ СОБРАННОГО APK ==========
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true  // Включаем BuildConfig
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"  // Обновил версию
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // Обновил до 17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true  // Для новых API на старых Android
    }
    
    kotlinOptions {
        jvmTarget = "17"  // Обновил до 17
    }
    
    // ========== ИМЕНА ВЫХОДНЫХ ФАЙЛОВ ==========
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            var fileName = "BLE-Spam-${variant.versionName}"
            
            when (variant.buildType.name) {
                "debug" -> fileName += "-DEBUG.apk"
                "release" -> fileName += "-RELEASE.apk"
                "staging" -> fileName += "-STAGING.apk"
                else -> fileName += ".apk"
            }
            
            output.outputFileName = fileName
        }
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
        jniLibs {
            useLegacyPackaging = true  // Для совместимости
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Material Design
    implementation("com.google.android.material:material:1.10.0")
    
    // БЛЕ (Bluetooth Low Energy) - если нужно явно
    implementation("androidx.bluetooth:bluetooth:1.0.0-alpha05")
    
    // Для новых Java API на старых Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    
    // Тестирование
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug инструменты
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ========== ДОПОЛНИТЕЛЬНЫЕ ЗАДАЧИ GRADLE ==========
tasks {
    // Задача для создания debug keystore если его нет
    register("createDebugKeystore") {
        group = "build"
        description = "Creates debug keystore if it doesn't exist"
        
        doLast {
            val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
            if (!debugKeystore.exists()) {
                exec {
                    commandLine = listOf(
                        "keytool",
                        "-genkey",
                        "-v",
                        "-keystore", debugKeystore.absolutePath,
                        "-storepass", "android",
                        "-alias", "androiddebugkey",
                        "-keypass", "android",
                        "-keyalg", "RSA",
                        "-keysize", "2048",
                        "-validity", "10000",
                        "-dname", "CN=Android Debug,O=Android,C=US"
                    )
                }
                println("✅ Debug keystore created at: ${debugKeystore.absolutePath}")
            } else {
                println("ℹ️ Debug keystore already exists")
            }
        }
    }
    
    // Задача для проверки подписи release
    register("verifyReleaseSignature") {
        group = "verification"
        description = "Verifies release APK signature"
        
        dependsOn("assembleRelease")
        
        doLast {
            val apkFile = file("app/build/outputs/apk/release/BLE-Spam-1.0-RELEASE.apk")
            if (apkFile.exists()) {
                exec {
                    commandLine = listOf(
                        "apksigner",
                        "verify",
                        "--verbose",
                        apkFile.absolutePath
                    )
                }
                println("✅ Release APK signature verified")
            } else {
                println("❌ Release APK not found")
            }
        }
    }
}

// ========== КОНФИГУРАЦИЯ ДЛЯ GITHUB ACTIONS ==========
// Можно задать параметры через командную строку:
// ./gradlew assembleRelease -PRELEASE_STORE_FILE=my.keystore -PRELEASE_STORE_PASSWORD=pass ...
