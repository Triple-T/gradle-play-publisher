package com.github.triplet.gradle.androidpublisher.internal

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.Apk
import com.google.api.services.androidpublisher.model.ApksListResponse
import com.google.api.services.androidpublisher.model.Bundle
import com.google.api.services.androidpublisher.model.BundlesListResponse
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class DefaultPlayPublisherTest {
    private val mockAndroidPublisher = mock(AndroidPublisher::class.java)
    private val appId = "appId"
    private val editId = "editId"
    
    private val mockEdits = mock(AndroidPublisher.Edits::class.java)
    
    private val mockBundles = mock(AndroidPublisher.Edits.Bundles::class.java)
    private val mockBundlesList = mock(AndroidPublisher.Edits.Bundles.List::class.java)
    private val mockBundlesResponse = mock(BundlesListResponse::class.java)
    
    private val mockApks = mock(AndroidPublisher.Edits.Apks::class.java)
    private val mockApksList = mock(AndroidPublisher.Edits.Apks.List::class.java)
    private val mockApksResponse = mock(ApksListResponse::class.java)

    private val publisher = DefaultPlayPublisher(mockAndroidPublisher, appId)

    @BeforeEach
    fun setUp() {
        `when`(mockAndroidPublisher.edits()).thenReturn(mockEdits)
        
        `when`(mockEdits.bundles()).thenReturn(mockBundles)
        `when`(mockBundles.list(appId, editId)).thenReturn(mockBundlesList)
        `when`(mockBundlesList.execute()).thenReturn(mockBundlesResponse)
        
        `when`(mockEdits.apks()).thenReturn(mockApks)
        `when`(mockApks.list(appId, editId)).thenReturn(mockApksList)
        `when`(mockApksList.execute()).thenReturn(mockApksResponse)
    }

    @Test
    fun `findMaxAppVersionCode returns 1 on null bundles and null apks`() {
        `when`(mockBundlesResponse.bundles).thenReturn(null)
        `when`(mockApksResponse.apks).thenReturn(null)

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(1)
    }

    @Test
    fun `findMaxAppVersionCode returns 1 on empty bundles and empty apks`() {
        `when`(mockBundlesResponse.bundles).thenReturn(emptyList())
        `when`(mockApksResponse.apks).thenReturn(emptyList())

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(1)
    }

    private fun bundle(versionCode: Int): Bundle {
        val bundle = Bundle()
        bundle.versionCode = versionCode
        return bundle
    }
    
    private fun apk(versionCode: Int): Apk {
        val apk = Apk()
        apk.versionCode = versionCode
        return apk
    }

    @Test
    fun `findMaxAppVersionCode succeeds with single bundle, no apks`() {
        `when`(mockBundlesResponse.bundles).thenReturn(listOf(bundle(5)))
        `when`(mockApksResponse.apks).thenReturn(emptyList())

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(5)
    }
    
    @Test
    fun `findMaxAppVersionCode succeeds with no bundles, single apk`() {
        `when`(mockBundlesResponse.bundles).thenReturn(emptyList())
        `when`(mockApksResponse.apks).thenReturn(listOf(apk(6)))

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(6)
    }

    @Test
    fun `findMaxAppVersionCode succeeds with multiple bundles and apks, max in bundles`() {
        `when`(mockBundlesResponse.bundles).thenReturn(
            listOf(
                bundle(5),
                bundle(12),
                bundle(8),
            )
        )
        `when`(mockApksResponse.apks).thenReturn(
            listOf(
                apk(3),
                apk(7),
                apk(2),
            )
        )

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(12)
    }
    
    @Test
    fun `findMaxAppVersionCode succeeds with multiple bundles and apks, max in apks`() {
        `when`(mockBundlesResponse.bundles).thenReturn(
            listOf(
                bundle(5),
                bundle(2),
                bundle(8),
            )
        )
        `when`(mockApksResponse.apks).thenReturn(
            listOf(
                apk(3),
                apk(15),
                apk(12),
            )
        )

        val max = publisher.findMaxAppVersionCode(editId)

        assertThat(max).isEqualTo(15)
    }
}
