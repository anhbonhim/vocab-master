plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    
    // For JSR-330 annotations (@Inject, @Singleton, etc.)
    implementation("javax.inject:javax.inject:1")

    testImplementation(libs.junit)
}
