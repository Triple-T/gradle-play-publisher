package de.triplet.gradle.play

class PlayRolloutFractionsExtension {

    def fractions = [0.005, 0.01, 0.05, 0.1, 0.2, 0.5, 1.0]

    double getNextUserFraction(double userFraction) {
        switch (userFraction) {
            case 0.005:
                return 0.01;
            case 0.01:
                return 0.05;
            case 0.05:
                return 0.1;
            case 0.1:
                return 0.2;
            case 0.2:
                return 0.5;
            case 0.5:
                return 1.0;
            default:
                return 1.0;
        }
    }

}
