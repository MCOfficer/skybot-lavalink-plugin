plugins {
    java
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("com.github.breadmoirai.github-release") version "2.2.12"
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val pluginVersion = Version(0, 0, 1)

group = "com.dunctebot"
version = "$pluginVersion"
// val archivesBaseName = "dunctebot-lavalink"

repositories {
    mavenCentral()
    maven("https://duncte123.jfrog.io/artifactory/maven")
    maven("https://m2.dv8tion.net/releases")
    maven("https://jitpack.io")
    jcenter() //koe :(
}

dependencies {
    implementation("com.dunctebot:sourcemanagers:1.5.7")

    compileOnly("com.github.freyacodes:Lavalink:2bd6b22")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val sourcesForRelease = task<Copy>("sourcesForRelease") {
    from("src/main/java") {
        include("**/Plugin.java")
        filter {
            it.replace(
            "0xFF1",
            "${pluginVersion.major}"
            )
            .replace(
                "0xFF2",
                "${pluginVersion.minor}")
            .replace(
                "0xFF3",
                "${pluginVersion.patch}"
            )
        }
    }

    into("build/filteredSrc")

    includeEmptyDirs = false
}

val generateJavaSources = task<SourceTask>("generateJavaSources") {
    val javaSources = sourceSets["main"].allJava.filter {
        !arrayOf("Plugin.java").contains(it.name)
    }.asFileTree

    source = javaSources + fileTree(sourcesForRelease.destinationDir)

    dependsOn(sourcesForRelease)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.dunctebot"
            artifactId = "lavalink"
            version = "$pluginVersion"

            from(components["java"])
        }
    }
}

tasks {
    compileJava {
        source = generateJavaSources.source

        dependsOn(generateJavaSources)
    }
    jar {
        archiveBaseName.set("dunctebot-plugin")
    }
    shadowJar {
        archiveBaseName.set("dunctebot-plugin")
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
    publish {
        dependsOn(publishToMavenLocal)
    }
}

data class Version(val major: Int, val minor: Int, val patch: Int) {
    override fun toString() = "$major.$minor.$patch"
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    owner("DuncteBot")
    repo("lavalink")
    tagName(pluginVersion.toString())
    overwrite(false)
    prerelease(System.getenv("PRERELEASE") == "true")
    body(changelog())
}