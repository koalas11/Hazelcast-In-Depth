
plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.guava)

    implementation(libs.apache.logging.api)
    implementation(libs.apache.logging.core)
    implementation(libs.apache.logging.slf4j2)

    implementation(libs.hazelcast)
    testImplementation(libs.hazelcast) {
        artifact {
            classifier = "tests"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass = "com.sanvito_damiano.hazelcast.Main"
    // The following JVM arguments are necessary for running Hazelcast with JDK 9+ with enhanced performance
    applicationDefaultJvmArgs = listOf(
        "--add-modules=java.se",
        "--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens=java.management/sun.management=ALL-UNNAMED",
        "--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED"
    )
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
