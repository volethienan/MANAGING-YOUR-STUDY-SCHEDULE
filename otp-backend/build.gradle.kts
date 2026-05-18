plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.example.otpbackend.OtpMailServer")
}

dependencies {
    implementation("com.sun.mail:jakarta.mail:2.0.1")
}
