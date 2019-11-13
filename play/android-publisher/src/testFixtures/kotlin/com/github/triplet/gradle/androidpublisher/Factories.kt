package com.github.triplet.gradle.androidpublisher

fun installPlayPublisherFactory(factory: PlayPublisher.Factory) = PlayPublisher.setFactory(factory)

fun installEditManagerFactory(factory: EditManager.Factory) = EditManager.setFactory(factory)
