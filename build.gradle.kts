plugins {
    java
    `maven-publish`
}

group = "de.dasbabypixel"
version = "1.0"

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components.getByName("java"))
        }
    }
    repositories {
        maven("https://nexus.darkcube.eu/repository/dasbabypixel/") {
            name = "DasBabyPixel"
            credentials(PasswordCredentials::class)
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        vendor = JvmVendorSpec.ADOPTIUM
    }
    withSourcesJar()
    withJavadocJar()
}