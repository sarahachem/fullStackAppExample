import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

val kotlinVersion = "1.4.0"
val serializationVersion = "1.0.0-RC"
val ktorVersion = "1.4.3"

plugins {
    kotlin("multiplatform") version "1.4.0"
    application //to run JVM part
    kotlin("kapt") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.0"
    id("org.springframework.boot") version "2.4.5"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
}

kotlin {
    jvm {
        withJava()
    }
    js {
        browser {
            binaries.executable()
        }
    }

    dependencies {
        implementation("com.github.mfarsikov:kotlite-core:0.3.1") // library containing annotations and classes used in compile time
        implementation("com.google.code.gson:gson:2.8.5")
        implementation("org.xerial:sqlite-jdbc:3.34.0")
        implementation ("org.springframework.boot:spring-boot-dependencies:2.1.0.RELEASE")
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("com.github.mfarsikov:kotlite-core:0.3.1") // library containing annotations and classes used in compile time
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:1.3.2")
            }
        }

        val jvmMain by getting {
            dependencies {
                jvm().compilations["main"].defaultSourceSet {
                    dependencies {
                        configurations["kapt"].dependencies.add(project.dependencies.create("com.github.mfarsikov:kotlite-kapt:0.3.1"))
                    }
                }
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.2.3")
                implementation("io.ktor:ktor-websockets:$ktorVersion")
                implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.1.1")

            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion") //include http&websockets

                //ktor client js json
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:1.3.2")

                implementation("org.jetbrains:kotlin-react:16.13.1-pre.110-kotlin-1.4.0")
                implementation("org.jetbrains:kotlin-react-dom:16.13.1-pre.110-kotlin-1.4.0")
                implementation(npm("react", "16.13.1"))
                implementation(npm("react-dom", "16.13.1"))
            }
        }
    }

    kapt {
        arguments {
            arg("kotlite.db.qualifiedName", "universe.db") // default database class name
            arg("kotlite.spring", "false") // marks database class as Spring's component
        }
    }
}

application {
    mainClassName = "ServerKt"
}

// include JS artifacts in any JAR we generate
tasks.getByName<Jar>("jvmJar") {
    val taskName = if (project.hasProperty("isProduction")) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) // bring output file along into the JAR
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}

distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

// Alias "installDist" as "stage" (for cloud providers)
tasks.create("stage") {
    dependsOn(tasks.getByName("installDist"))
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}