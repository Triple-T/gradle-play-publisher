## Want to contribute? Awesome!

### Building the plugin

1. Run `./gradlew publishToMavenLocal`
1. Make your changes
1. Run the tests with `./gradlew test`
1. Ensure your changes work in a live environment: `(cd testapp && ../gradlew taskName)`, e.g.
   `(cd testapp && ../gradlew publishBundle)`

### Adding new features

Before you start working on a larger contribution, you should get in touch with
us first through the issue tracker with your idea so that we can help out and
possibly guide you. Coordinating up front makes it much easier to avoid
frustration later on.

If this has been discussed in an issue, make sure to mention the issue number.
If not, go file an issue about this to make sure this is a desirable change.

### Code reviews

All submissions, including submissions by project members, require review. We
adhere to the
[Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html).
