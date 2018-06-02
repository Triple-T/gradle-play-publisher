buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.vanniktech:gradle-maven-publish-plugin:0.2.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
