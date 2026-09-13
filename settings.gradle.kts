pluginManagement {
    includeBuild("build-logic")
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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "locationjoystick"

include(":app")

include(":lint:checks")

include(":core:common")
include(":core:data")
include(":core:map")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:location")
include(":core:model")
include(":core:overlay")
include(":core:routing")
include(":core:testing")

include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:map:api")
include(":feature:map:impl")
include(":feature:joystick:impl")
include(":feature:routes:api")
include(":feature:routes:impl")
include(":feature:favorites:api")
include(":feature:favorites:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
include(":feature:widget:impl")
include(":feature:group:api")
include(":feature:group:impl")
