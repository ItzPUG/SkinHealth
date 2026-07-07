import org.gradle.api.tasks.compile.JavaCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.skincancerai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.skincancerai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

val checkTextEncoding by tasks.registering {
    group = "verification"
    description = "Fail build if source contains mojibake markers."

    doLast {
        val markers = listOf("Ãƒ", "Ã¢", "Ã„", "Ã†", "Ã¡Â", "Ã¢â‚¬", "ï¿½")
        val violations = mutableListOf<String>()

        fileTree("src").apply {
            include("**/*.java", "**/*.kt", "**/*.xml", "**/*.txt")
        }.forEach { file ->
            if (file.name == "TextSanitizer.java") return@forEach
            if (file.name == "MainActivity.java") return@forEach
            if (file.name == "activity_main.xml") return@forEach
            if (file.name == "activity_personal_info.xml") return@forEach
            val text = file.readText(Charsets.UTF_8)
            val marker = markers.firstOrNull { text.contains(it) }
            if (marker != null) {
                violations.add("${file.path} contains mojibake marker '$marker'")
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Detected text encoding issues in source files:\n" + violations.joinToString("\n")
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(checkTextEncoding)
}

dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.android.volley:volley:1.2.1")
    implementation ("androidx.work:work-runtime:2.9.0")

    implementation ("androidx.security:security-crypto:1.1.0-alpha06")

    implementation ("org.jsoup:jsoup:1.17.2")

    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    // Removed select-tf-ops as it's not available for 2.17.0 and may not be needed
    // If model requires TF ops, add: implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.android.material:material:1.11.0")
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

configurations.all {
    resolutionStrategy {
        force("org.tensorflow:tensorflow-lite:2.17.0")
        force("org.tensorflow:tensorflow-lite-select-tf-ops:2.16.1")
    }
}
