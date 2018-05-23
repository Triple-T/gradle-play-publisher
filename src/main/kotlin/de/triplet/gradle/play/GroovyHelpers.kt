package de.triplet.gradle.play

import com.android.builder.model.ProductFlavor
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension

fun ProductFlavor.setExtra(name: String, value: Any?) {
    ((this as ExtensionAware).extensions
            .getByName("ext") as ExtraPropertiesExtension)
            .set(name, value)
}

fun <T> ProductFlavor.getExtra(name: String): T? {
    return ((this as ExtensionAware).extensions
            .getByName("ext") as ExtraPropertiesExtension)
            .get(name) as? T
}