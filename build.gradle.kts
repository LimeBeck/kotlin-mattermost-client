plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.publish)
    alias(libs.plugins.versions)
    alias(libs.plugins.dokka)
}

val libVersion: String by project
group = "dev.limebeck.libs"
version = libVersion

subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "com.vanniktech.maven.publish")

    mavenPublishing {
        publishToMavenCentral()
        signAllPublications()

        pom {
            url.set("https://github.com/LimeBeck/kotlin-mattermost-client")
            description.set("Kotlin Client for Mattermost")
            developers {
                developer {
                    id.set("LimeBeck")
                    name.set("Anatoly Nechay-Gumen")
                    email.set("mail@limebeck.dev")
                }
            }
            licenses {
                license {
                    name.set("MIT license")
                    url.set("https://github.com/LimeBeck/kotlin-mattermost-client/blob/master/LICENCE")
                    distribution.set("repo")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/LimeBeck/kotlin-mattermost-client.git")
                developerConnection.set("scm:git:ssh://github.com/LimeBeck/kotlin-mattermost-client.git")
                url.set("https://github.com/LimeBeck/kotlin-mattermost-client")
            }
        }
    }
}

dokka {
    moduleName.set("Kotlin Mattermost Client")

    dokkaPublications.html {
        suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }

    dokkaSourceSets.configureEach {
        includes.from("README.MD")
    }

    pluginsConfiguration.html {
        footerMessage.set("(c) LimeBeck.Dev")
    }
}

dependencies {
    dokka(project(":mattermost-client"))
}
