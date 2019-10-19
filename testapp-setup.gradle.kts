private val publicGppTasks = listOf(
        "bootstrap",
        "bootstrapRelease",
        "promoteArtifact",
        "promoteReleaseArtifact",
        "publish",
        "publishApk",
        "publishBundle",
        "publishListing",
        "publishProducts",
        "publishRelease",
        "publishReleaseApk",
        "publishReleaseBundle",
        "publishReleaseListing",
        "publishReleaseProducts",
        "uploadReleasePrivateApk",
        "uploadReleasePrivateBundle"
)

for (name in publicGppTasks) {
    tasks.register("testapp${name.capitalize()}") {
        dependsOn(gradle.includedBuild("testapp").task(":$name"))
    }
}
