package de.triplet.gradle.play

class PlayRolloutFractionsExtension {

    def fractions = [0.005, 0.01, 0.05, 0.1, 0.2, 0.5, 1.0]

    double getNextUserFraction(double userFraction) {
        return 0.01
    }

}
