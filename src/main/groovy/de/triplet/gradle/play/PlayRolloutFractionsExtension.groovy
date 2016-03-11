package de.triplet.gradle.play

class PlayRolloutFractionsExtension {

    def fractions = [0.005d, 0.01d, 0.05d, 0.1d, 0.2d, 0.5d, 1.0d]

    double getNextUserFraction(double userFraction) {
        if (userFraction == 1.0d) {
            return 1.0d;
        } else {
            int userFractionIndex = fractions.indexOf(userFraction);
            return fractions.get(userFractionIndex + 1);
        }
    }

}
