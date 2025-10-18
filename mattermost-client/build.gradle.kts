@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

kotlin {
    // JVM target
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // JS target
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        nodejs()
    }

    // WASM target is not available, because limebeck.libs not published it yet
//    wasmJs {
//        browser()
//        nodejs()
//        binaries.library()
//    }

    // Native target (Linux)
    linuxX64 {
        binaries {
            sharedLib()
            staticLib()
        }
    }

    sourceSets {
        // Common dependencies
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.client.websockets)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.limebeck.common)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        // JVM dependencies
        val jvmMain by getting {
            dependencies {
            }
        }

        val jvmTest by getting {
            dependencies {
            }
        }

        // JS dependencies
        val jsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }

        val jsTest by getting {
            dependencies {
            }
        }

        // WASM dependencies
//        val wasmJsMain by getting {
//            dependencies {
//                implementation(libs.ktor.client.js)
//            }
//        }
//
//        val wasmJsTest by getting

        // Native dependencies
        val linuxX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        val linuxX64Test by getting {
            dependencies {
            }
        }
    }
}
