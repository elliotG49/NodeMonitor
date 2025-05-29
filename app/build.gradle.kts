plugins {
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.beryx.jlink") version "2.25.0"
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.example.app.NetworkMonitorApp")
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java")
        }
        resources {
            srcDirs("src/main/resources")
        }
    }
    test {
        java {
            srcDirs("src/test/java")
        }
    }
}

// Disable the standard jar task so it doesn't interfere.
tasks.jar {
    enabled = false
}

// Configure the Shadow Jar task to build a fat jar without a classifier.
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

// Reconfigure startScripts so that it depends on shadowJar and uses its output.
tasks.startScripts {
    dependsOn(tasks.shadowJar)
    // Override the classpath so the launch scripts use the fat jar.
    classpath = files(tasks.shadowJar.get().archiveFile)
}

// Configure jlink to use the fat jar from the shadowJar task.
jlink {
    launcher {
        name = "Network Node Monitor"
        jvmArgs = listOf("--add-reads", "org.example=ALL-UNNAMED")
    }
    jpackage {
        installerType = "exe"  // Windows installer.
        imageOptions = listOf("--icon", "C:\\Users\\ellio\\Desktop\\network-monitor\\app\\src\\main\\resources\\icons\\Nodes.ico")
    }
}
