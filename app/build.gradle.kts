import java.util.Properties
///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("com.diffplug.spotless")
}

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
val kotlinVersion: String by project
val archivesBaseName: String by project
val signingPropertiesFile = File("signing.properties")
android {
    compileSdkVersion(29)
    buildToolsVersion("30.0.3")

    signingConfigs {
        create("default") {
            // If the application has to be signed, the elements necessary for this operation
            // must be defined in a 'signing.properties' file placed at the root of the project.
            if (signingPropertiesFile.exists()) {
                val properties = Properties().apply {
                    load(signingPropertiesFile.reader())
                }
                storeFile = File(properties.getProperty("storeFilePath"))
                storePassword = properties.getProperty("storePassword")
                keyPassword = properties.getProperty("keyPassword")
                keyAlias = properties.getProperty("keyAlias")
            }
        }
    }

    defaultConfig {
        applicationId("org.calypsonet.keyple.demo.control")
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode(6)
        versionName(project.version.toString())
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("default")
            }
        }
    }

    val javaSourceLevel: String by project
    val javaTargetLevel: String by project
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled  = true
        sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
        targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    }

    packagingOptions {
        exclude("META-INF/NOTICE.md")
        exclude("META-INF/plugin_release.kotlin_module")
    }

    kotlinOptions {
        jvmTarget = javaTargetLevel
    }

    lintOptions {
        isAbortOnError = false
    }

    // generate output aar with a qualified name: with version number
    applicationVariants.all {
        outputs.forEach { output ->
            if (output is com.android.build.gradle.internal.api.BaseVariantOutputImpl) {
                output.outputFileName = "${archivesBaseName}-${project.version}-${buildType.name}.${output.outputFile.extension}".replace("-SNAPSHOT", "")
            }
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}

dependencies {
    // Demo common
    implementation("org.calypsonet.keyple:keyple-demo-common-lib:2.0.0-SNAPSHOT") { isChanging = true }

    // Keyple reader plugins proprietary libs
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // Keyple core
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.3.0")
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.8.0")
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.0")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.3.0")
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.3.1")
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.3.5")

    // Keyple reader plugins
    implementation("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.1")
    implementation("org.calypsonet.keyple:keyple-plugin-cna-coppernic-cone2-java-lib:2.0.2")
    implementation("org.calypsonet.keyple:keyple-plugin-cna-famoco-se-communication-java-lib:2.0.2")

    // Android components
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.2")
    implementation("androidx.activity:activity-ktx:1.2.1")
    implementation("androidx.fragment:fragment-ktx:1.3.1")

    // Log
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.arcao:slf4j-timber:3.1@aar") //SLF4J binding for Timber

    // Kotlin
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.3")

    implementation("androidx.multidex:multidex:2.0.1")

    // RxJava
    implementation("io.reactivex.rxjava2:rxjava:2.1.13")
    implementation("io.reactivex.rxjava2:rxandroid:2.0.2")

    // Google GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // Devnied - Byte Utils
    implementation("com.github.devnied:bit-lib4j:1.4.5") {
        exclude(group = "org.slf4j")
    }

    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("com.jakewharton.timber:timber:4.7.1") //Android
    implementation("com.arcao:slf4j-timber:3.1@aar") //SLF4J binding for Timber

    // Dagger dependencies
    kapt("com.google.dagger:dagger-compiler:2.19")
    annotationProcessor("com.google.dagger:dagger-compiler:2.19")
    kapt("com.google.dagger:dagger-android-processor:2.19")
    annotationProcessor("com.google.dagger:dagger-android-processor:2.19")
    implementation("com.google.dagger:dagger:2.19")
    implementation("com.google.dagger:dagger-android:2.19")
    implementation("com.google.dagger:dagger-android-support:2.19")
    compileOnly("org.glassfish:javax.annotation:10.0-b28")

    // Common lang
    implementation("org.apache.commons:commons-lang3:3.11")

    // Lottie
    implementation("com.airbnb.android:lottie:3.4.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.3.1")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.0")
}

apply(plugin = "org.eclipse.keyple") // To do last