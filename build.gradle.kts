plugins {
    id("com.gradleup.shadow") version "8.3.6"
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
    id ("org.gradlex.reproducible-builds") version "1.0"
}


version = "0.0.1"
group = "io.github.vpa1977"


repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
   archiveClassifier.set("")
}

dependencies {
    implementation(libs.commons.text)
    implementation(libs.maven.model)
    implementation(libs.maven.model.builder)

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

gradlePlugin {
    website = "https://github.com/vpa1977/gradle-buildinspector"
    vcsUrl = "https://github.com/vpa1977/gradle-buildinspector"

    plugins {
        create("buildInspectorPlugin") {
            id = "io.github.vpa1977.buildinspector"
            displayName = "Build Inspector Plugin"
            description = "Investigate gradle build configuration"
            implementationClass = "com.canonical.buildinspector.BuildInspectorPlugin"
            tags = listOf("gradle", "build", "inspector")
        }
    }
}

tasks.named<Test>("test") {
    // Use JUnit Jupiter for unit tests.
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Xlint:all")
}
