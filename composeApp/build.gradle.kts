import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.filekit.compose)
            implementation(libs.filekit.core)
            implementation(libs.exposed.core)
            implementation(libs.exposed.dao)
            implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.compose.material:material:1.7.3")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.poi)
            implementation(libs.poi.ooxml)
            implementation(libs.poi.scratchpad)
            implementation(libs.exposed.jdbc)
            implementation(libs.sqlite.jdbc)
            implementation(libs.gson)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.example.poststudy.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "BreakPoint"
            // Raise for every release sent to users: an MSI with the same version does not replace the installed one
            packageVersion = "1.1.1"
            vendor = "BreakPoint Vendor"
            copyright = "© 2024 BreakPoint"
            description = "BreakPoint"

            // Checked with `./gradlew suggestRuntimeModules`; java.prefs is needed by the connect screen,
            // jdk.charsets by legacy .doc files in Cyrillic code pages
            modules(
                "java.sql", "java.naming", "java.desktop", "java.management", "java.xml", "java.logging",
                "java.prefs", "java.instrument", "java.security.jgss", "java.xml.crypto",
                "jdk.unsupported", "jdk.httpserver", "jdk.security.auth", "jdk.charsets", "jdk.localedata"
            )

            windows {
                shortcut = true
                menu = true
                menuGroup = "BreakPoint"
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/icon.ico"))
                upgradeUuid = "8a63c076-2146-45ef-8fa6-d90b4a020700"
                perUserInstall = true
            }
        }
    }
}