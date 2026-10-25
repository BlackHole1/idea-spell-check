import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
    id("jacoco") // JaCoCo code coverage
}

kotlin {
    compilerOptions {
        // The plugin does not bundle its own Kotlin stdlib (see `kotlin.stdlib.default.dependency` in gradle.properties),
        // so the API level must match the stdlib bundled by the oldest supported IDE (2026.2 ships Kotlin 2.4.0).
        // Without this, the compiler emits calls to newer stdlib symbols (e.g. `kotlin.coroutines.jvm.internal.SpillingKt`,
        // API >= 2.2) that do not exist at runtime. Bundled versions: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
        apiVersion = KotlinVersion.KOTLIN_2_4
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("net.mamoe.yamlkt:yamlkt-jvm:0.13.0")
    implementation("com.akuleshov7:ktoml-core:0.7.1")
    implementation("com.akuleshov7:ktoml-file:0.7.1")

    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.2.3")
        bundledModule("intellij.spellchecker")
        testFramework(TestFrameworkType.Platform)
        // Unified IntelliJ IDEA (2025.3+) needs the ultimate module on the test classpath for the platform test fixtures
        testBundledPlugins("com.intellij.modules.ultimate")
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }
    }

    publishing {
        // The plugin version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = listOf(project.version.toString().substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
    }
}

tasks {
    test {
        extensions.configure(JacocoTaskExtension::class) {
            isIncludeNoLocationClasses = true
            includes = listOf("com.github.blackhole1.ideaspellcheck.*")
        }
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required = true
            html.required = true
            csv.required = false
        }
    }

    check {
        dependsOn(jacocoTestReport)
    }
}

// Configure code coverage using Gradle's built-in JaCoCo integration.
jacoco {
    toolVersion = "0.8.15"
}
