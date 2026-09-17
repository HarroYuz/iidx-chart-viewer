import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val sharedSigningProperties = rootProject.file("signing/signing.properties")
val sharedSigning = Properties().takeIf { sharedSigningProperties.isFile }?.apply {
    sharedSigningProperties.inputStream().use(::load)
}
val sharedSigningFile = sharedSigning?.getProperty("storeFile")?.let(rootProject::file)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.harroyuz.iidxchartviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harroyuz.iidxchartviewer"
        minSdk = 26
        targetSdk = 36
        versionCode = 33
        versionName = "1.2.3"
    }

    signingConfigs {
        if (sharedSigning != null && sharedSigningFile != null) {
            create("sharedDebug") {
                storeFile = sharedSigningFile
                storePassword = sharedSigning.getProperty("storePassword")
                keyAlias = sharedSigning.getProperty("keyAlias")
                keyPassword = sharedSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (sharedSigning != null) signingConfig = signingConfigs.getByName("sharedDebug")
        }
        getByName("debug") {
            if (sharedSigning != null) signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.mozilla:rhino:1.7.15")
    implementation("org.jsoup:jsoup:1.23.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}

// Opt-in, offline comparison of public catalog snapshots using the app's real matching code.
tasks.register<JavaExec>("auditCatalog") {
    dependsOn("compileDebugUnitTestKotlin", "processDebugUnitTestJavaRes")
    mainClass.set("com.harroyuz.iidxchartviewer.tools.CatalogAudit")
    doFirst {
        classpath = tasks.named<Test>("testDebugUnitTest").get().classpath
        args(
            providers.gradleProperty("auditTextage").get(),
            providers.gradleProperty("auditBjm").get(),
            providers.gradleProperty("auditOutput").get(),
        )
    }
}
