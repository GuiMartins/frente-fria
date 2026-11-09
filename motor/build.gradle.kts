// Motor: Kotlin puro, sem Android. Toda regra (previsão, fases, nível de alerta,
// insights, conteúdo do dia) mora aqui, para os testes rodarem na JVM em segundos.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    // O conteúdo que vai no APK é validado pelos testes do motor.
    systemProperty("conteudo.dir", rootProject.file("app/src/main/assets/conteudo").absolutePath)
}
