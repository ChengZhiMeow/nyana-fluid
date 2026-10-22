allprojects {
    group = property("group") as String
    version = property("version") as String

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.momirealms.net/releases/")
        maven("https://repo.momirealms.net/snapshots/")
    }
}

subprojects {
    if (!file("src").exists()) return@subprojects
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-Xlint:all,-processing,-serial", "-parameters"))
    }

    tasks.withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
    }

    dependencies {
        "compileOnly"("org.jetbrains:annotations:${property("jetbrainsAnnotationsVersion")}")
    }
}
