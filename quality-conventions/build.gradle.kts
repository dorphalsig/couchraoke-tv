plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.couchraoke"
version = "1.0.0"

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

dependencies {
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
    implementation("io.gitlab.arturbosch.detekt:detekt-api:1.23.8")
    implementation("org.jacoco:org.jacoco.core:0.8.12")
    implementation("io.github.takahirom.roborazzi:roborazzi-gradle-plugin:1.59.0")
    compileOnly("com.android.tools.build:gradle:9.1.0")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
}

gradlePlugin {
    plugins {
        create("qualityConventions") {
            id = "com.couchraoke.quality-conventions"
            implementationClass = "com.couchraoke.quality.QualityConventionsPlugin"
        }
    }
}

// Java and Kotlin versions
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
