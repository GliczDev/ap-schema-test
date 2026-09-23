allprojects {
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}

dependencies {
    "compileOnly"(project(":ap"))
    "annotationProcessor"(project(":ap"))
}
