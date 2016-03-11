package de.triplet.gradle.play

class PlayRolloutFractionsExtension {

    def fullRelease = 1.0d;

    def fractions = [0.005d, 0.01d, 0.05d, 0.1d, 0.2d, 0.5d, fullRelease]

    double getNextUserFraction(double userFraction) {
        if (userFraction == fullRelease) {
            return fullRelease;
        } else {
            int userFractionIndex = fractions.indexOf(userFraction);
            return fractions.get(userFractionIndex + 1);
        }
    }

}
