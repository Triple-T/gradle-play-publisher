package de.triplet.gradle.play

class ImageFileFilter implements FileFilter {
    @Override
    boolean accept(File pathname) {
        return pathname.name.toLowerCase().endsWith('.png') ||
                pathname.name.toLowerCase().endsWith('.jpg')
    }
}
