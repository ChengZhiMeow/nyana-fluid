plugins {
    `java-library`
    `maven-publish`
}

java {
    withJavadocJar()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${property("paperApiVersion")}")
    compileOnly("net.momirealms:craft-engine-core:${property("craftEngineVersion")}")
    compileOnly("net.momirealms:craft-engine-bukkit:${property("craftEngineVersion")}")
}

tasks.javadoc {
    exclude("net/nyana/fluid/api/internal/**")
    (options as StandardJavadocDocletOptions).apply {
        docTitle = "nyana-fluid API ${project.version}"
        windowTitle = "nyana-fluid API ${project.version}"
        locale = "zh_CN"
        charSet = "UTF-8"
        // Comments are optional; broken links and HTML still fail the build.
        addBooleanOption("Xdoclint:all,-missing", true)
        addBooleanOption("Xwerror", true)
    }
}

tasks.jar {
    archiveBaseName.set("nyana-fluid-api")
}

publishing {
    repositories {
        maven {
            name = "Catnies"
            url = uri("https://repo.catnies.top/releases")
            credentials(PasswordCredentials::class)
            authentication { create<BasicAuthentication>("basic") }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifactId = "nyana-fluid-api"
            from(components["java"])
            pom {
                name.set("nyana-fluid API")
                description.set("Fluid transfer API for Paper and CraftEngine plugins")
            }
        }
    }
}
