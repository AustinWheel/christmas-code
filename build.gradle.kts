import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "dev.austin.christmas"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
    intellijPlatform { defaultRepositories() }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    intellijPlatform {
        val localIde = providers.gradleProperty("ideaPath")
        if (localIde.isPresent) local(localIde.get()) else webstorm("2025.3.2")
        pluginVerifier()
        zipSigner()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    instrumentCode = false
    pluginVerification {
        val reportKey = providers.gradleProperty("verifyProduct").getOrElse("local") + "-" +
            providers.gradleProperty("verifyVersion").getOrElse("installed")
        verificationReportsDirectory = layout.buildDirectory.dir("reports/pluginVerifier/$reportKey")
        ides {
            val localVerify = providers.gradleProperty("verifyIdePath")
            if (localVerify.isPresent) local(file(localVerify.get()))
            else create(
                IntelliJPlatformType.fromCode(providers.gradleProperty("verifyProduct").getOrElse("WS"))!!,
                providers.gradleProperty("verifyVersion").getOrElse("2026.3")
            )
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }
    pluginConfiguration {
        ideaVersion { sinceBuild = "253"; untilBuild = "263.*" }
    }
}

tasks.register<JavaExec>("preview") {
    group = "application"
    description = "Run the animated scene in a standalone Swing window."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.austin.christmas.scene.ScenePreview"
}

tasks.register<JavaExec>("sceneCheck") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.austin.christmas.scene.ScenePreview"
    args("--check")
    systemProperty("java.awt.headless", "true")
}


tasks.register<JavaExec>("sceneSnapshot") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.austin.christmas.scene.ScenePreview"
    args("--snapshot")
    systemProperty("java.awt.headless", "true")
}


tasks.register<JavaExec>("sceneTurntable") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "dev.austin.christmas.scene.ScenePreview"
    args("--turntable")
    systemProperty("java.awt.headless", "true")
}

// Ship the license with the binary distribution as well as the source repository.
tasks.jar {
    from(files("LICENSE", "THIRD_PARTY_NOTICES.md")) { into("META-INF") }
}

// Gradle 9 requires this dependency to validate the signed archive's producer.
tasks.named("verifyPluginSignature") { dependsOn("signPlugin") }
