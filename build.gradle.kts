///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.diffplug.spotless") version "5.10.2"
}
buildscript {
    val kotlinVersion: String by project
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}
apply(plugin = "org.eclipse.keyple")

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
allprojects {
    group = "org.calypsonet.keyple"
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://oss.sonatype.org/content/repositories/releases")
        maven(url = "https://nexus.coppernic.fr/repository/libs-release")
        google()
    }
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        kotlin {
            target("**/*.kt")
            ktfmt()
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
        }
    }
}
