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
android {
    compileSdkVersion(29)
    buildToolsVersion("30.0.2")

    defaultConfig {
        applicationId("org.calypsonet.keyple.demo.control.di")
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode(6)
        versionName(project.version.toString())

        testInstrumentationRunner("android.support.test.runner.AndroidJUnitRunner")
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val javaSourceLevel: String by project
    val javaTargetLevel: String by project
    compileOptions {
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

    /*
    flavorDimensions("device")
    productFlavors {
        create("omapi") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control OMAPI")
            applicationIdSuffix = ".omapi"
        }
        create("coppernic") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control Coppernic")
            applicationIdSuffix = ".coppernic"
        }
        create("famoco") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control Famoco")
            applicationIdSuffix = ".famoco"
        }
        create("mockSam") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control Mock Sam")
            applicationIdSuffix = ".mockSam"
        }
        create("bluebird") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control Bluebird")
            applicationIdSuffix = ".bluebird"
        }
        create("flowbird") {
            dimension = "device"
            resValue("string", "app_name", "Keyple Control Flowbird")
            applicationIdSuffix = ".flowbird"
        }
    }
    */

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")

        /*getByName("omapi").java.srcDirs("src/omapi/kotlin")
        getByName("coppernic").java.srcDirs("src/coppernic/kotlin")
        getByName("famoco").java.srcDirs("src/famoco/kotlin")
        getByName("mockSam").java.srcDirs("src/mockSam/kotlin")
        getByName("bluebird").java.srcDirs("src/bluebird/kotlin")
        getByName("flowbird").java.srcDirs("src/flowbird/kotlin")*/
    }
}

dependencies {
    // Demo common
    implementation("org.calypsonet.keyple:keyple-demo-common-lib:1.0.0-SNAPSHOT") { isChanging = true }

    // Keyple
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.+") { isChanging = true }
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.2.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.1.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-resource-java-lib:2.0.2-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.2.0")

    implementation("org.calypsonet.keyple:keyple-plugin-cna-bluebird-specific-nfc-java-lib:2.0.0-rc1")

    /*
    "omapiImplementation"("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.1")
    "omapiImplementation"("org.eclipse.keyple:keyple-plugin-android-omapi-java-lib:2.0.1")

    "famocoImplementation"("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.0-rc3")
    "famocoImplementation"("org.calypsonet.keyple:keyple-plugin-cna-famoco-se-communication-java-lib:2.0.0-rc1")

    "mockSamImplementation"("org.eclipse.keyple:keyple-plugin-android-nfc-java-lib:2.0.1")

    "coppernicImplementation"("org.calypsonet.keyple:keyple-plugin-cna-coppernic-cone2-java-lib:2.0.0-rc1")

    "bluebirdImplementation"("org.calypsonet.keyple:keyple-plugin-cna-bluebird-specific-nfc-java-lib:2.0.0-rc1")

    "flowbirdImplementation"("org.calypsonet.keyple:keyple-plugin-cna-flowbird-android-java-lib:2.0.0-rc1")
    */

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

    // Joda Time
    implementation("joda-time:joda-time:2.8.1")

    // Google GSON
    implementation("com.google.code.gson:gson:2.8.6")

    // Devnied - Byte Utils
    implementation("com.github.devnied:bit-lib4j:1.4.5") {
        exclude(group = "org.slf4j")
    }

    implementation("org.slf4j:slf4j-api:1.7.25")
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

    testImplementation("junit:junit:4.12")
    testImplementation("org.robolectric:robolectric:4.3.1")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}