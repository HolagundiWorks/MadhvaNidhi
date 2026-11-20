// Pure Kotlin, no Android dependencies: calendar traditions, Kannada/Sanskrit text helpers, catalogue
// classification and count maths. Shared by Madhva Nidhi and Madhva Dvija.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
