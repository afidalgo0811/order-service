import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    id("org.springframework.boot") version "3.1.3"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
    id("com.diffplug.spotless") version "6.19.0"
    kotlin("kapt") version "1.8.0"
}

group = "com.afidalgo"

version = "0.0.1-SNAPSHOT"

extra["springCloudVersion"] = "2022.0.3"

extra["testcontainersVersion"] = "1.18.0"

java { sourceCompatibility = JavaVersion.VERSION_17 }

java { targetCompatibility = JavaVersion.VERSION_17 }

val spaceUsername: String? by project
val spacePassword: String? by project
val userName: String? = System.getenv("SPACE_USERNAME")
val passWord: String? = System.getenv("SPACE_PASSWORD")
val usr = userName ?: spaceUsername // checks env first
val psw = passWord ?: spacePassword // checks env first
val urlArtifactRepository = ext["jetbrains.url"].toString()
val sharedLibraryVersion = ext["shared.library.version"].toString()

repositories {
    mavenCentral()
    maven {
        url = uri(urlArtifactRepository)
        credentials {
            username = usr
            password = psw
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")
    runtimeOnly("org.flywaydb:flyway-core")
    runtimeOnly("org.springframework:spring-jdbc")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.retry:spring-retry")
    testImplementation("com.squareup.okhttp3:mockwebserver")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.afidalgo:shared-library:$sharedLibraryVersion")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

configure<SpotlessExtension> {
    kotlin {
        // by default the target is every '.kt' and '.kts` file in the java sourcesets
        ktfmt() // has its own section below
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt()
    }
}

tasks.withType<Test> { useJUnitPlatform() }

dependencyManagement {
    imports {
        mavenBom(
            "org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set(project.name)
    environment.set(environment.get() + mapOf("BP_JVM_VERSION" to "17"))
    docker {
        publishRegistry {
            username.set(project.findProperty("registryUsername").toString())
            password.set(project.findProperty("registryToken").toString())
            url.set(project.findProperty("registryUrl").toString())
        }
    }
}