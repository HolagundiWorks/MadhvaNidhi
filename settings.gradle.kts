pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MadhvaNidhi"
include(":app")
include(":designsystem")
project(":designsystem").projectDir = file("../DesignSystem/designsystem")
include(":shared:core")
include(":dvija")
