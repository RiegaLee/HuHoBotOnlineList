import java.util.zip.ZipFile

plugins {
    java
}

group = "cn.huohuas001.huhobot.addons"
version = "1.22.0"

val huhobotQqSdkJar = providers.gradleProperty("huhobotQqSdkJar")
    .orElse(providers.environmentVariable("HUHOBOT_QQ_SDK_JAR"))
    .orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let(::file)
    ?: file("../PenguinClient-Main/common/Bot/build/libs/common-Bot-1.5.0.jar")

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly(files(huhobotQqSdkJar))
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation(files(huhobotQqSdkJar))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

tasks.processResources {
    // Gradle's filtering otherwise follows the Windows system code page. Paper always reads plugin.yml as UTF-8.
    filteringCharset = "UTF-8"
    val values = mapOf("version" to project.version)
    inputs.properties(values)
    filesMatching("plugin.yml") {
        expand(values)
    }
}

tasks.jar {
    archiveFileName.set("HuHoBot-OnlineList-${project.version}.jar")
}

tasks.test {
    useJUnitPlatform()
    include("**/*Test.class")
    systemProperty("java.awt.headless", "true")
}

val verifyAddonJar by tasks.registering {
    group = "verification"
    description = "Checks the Spigot-safe addon boundary and required resources."
    dependsOn(tasks.jar)

    doLast {
        check(huhobotQqSdkJar.isFile) {
            "Build PenguinClient-Main/common/Bot first; expected ${huhobotQqSdkJar.absolutePath}"
        }
        ZipFile(tasks.jar.get().archiveFile.get().asFile).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            listOf(
                "plugin.yml",
                "config.yml",
                "fonts/HuHoBotOnlineTitle-Semibold.ttf"
            ).forEach { required ->
                check(required in entries) { "Addon JAR is missing $required" }
            }
            check(entries.none { it.startsWith("online/huhobot-glass-") }) {
                "Legacy robot-theme image resources must not remain in the rebuilt addon"
            }
            check(entries.none {
                it.startsWith("cn/huohuas001/bot/") ||
                    it.startsWith("io/github/kloping/") ||
                    it.startsWith("org/bukkit/") ||
                    it.startsWith("io/papermc/") ||
                    it.startsWith("net/minecraft/")
            }) {
                "Addon JAR must not bundle HuHoBot, QQ SDK, Bukkit, Paper, or NMS classes"
            }
        }
    }
}

tasks.build {
    dependsOn(verifyAddonJar)
}
