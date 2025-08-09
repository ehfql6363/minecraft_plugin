import java.util.Properties

plugins {
    java
}

group = "kwangdong"
version = "1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-text-serializer-legacy:4.14.0")
    implementation("net.kyori:adventure-platform-bukkit:4.3.2")
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val pluginYml = file("src/main/resources/plugin.yml")
    val props = Properties()
    props.load(pluginYml.inputStream())

    val pluginName = props.getProperty("name")
    val pluginVersion = props.getProperty("version")

    archiveBaseName.set(pluginName)
    archiveVersion.set(pluginVersion)

    from("src/main/resources") {
        include("plugin.yml")
    }
}
