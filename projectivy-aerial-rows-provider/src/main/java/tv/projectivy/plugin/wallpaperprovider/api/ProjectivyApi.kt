/*
 * Projectivy wallpaper-provider API model, adapted from
 * https://github.com/spocky/projectivy-plugin-wallpaper-overflight
 * Copyright the Projectivy contributors. Licensed under Apache-2.0.
 */
package tv.projectivy.plugin.wallpaperprovider.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.parcelize.Parcelize

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    WallpaperType.IMAGE,
    WallpaperType.DRAWABLE,
    WallpaperType.ANIMATED_DRAWABLE,
    WallpaperType.LOTTIE,
    WallpaperType.VIDEO,
    WallpaperType.COLOR,
)
annotation class WallpaperType {
    companion object {
        const val IMAGE = 0
        const val DRAWABLE = 1
        const val ANIMATED_DRAWABLE = 2
        const val LOTTIE = 3
        const val VIDEO = 4
        const val COLOR = 5
    }
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    WallpaperDisplayMode.DEFAULT,
    WallpaperDisplayMode.CROP,
    WallpaperDisplayMode.STRETCH,
    WallpaperDisplayMode.BLUR,
    WallpaperDisplayMode.FIT_CENTER,
    WallpaperDisplayMode.FIT_CENTER_BLURRED_BG,
)
annotation class WallpaperDisplayMode {
    companion object {
        const val DEFAULT = 0
        const val CROP = 1
        const val STRETCH = 2
        const val BLUR = 3
        const val FIT_CENTER = 4
        const val FIT_CENTER_BLURRED_BG = 5
    }
}

@Parcelize
data class Wallpaper(
    val uri: String,
    val type: @WallpaperType Int = WallpaperType.IMAGE,
    val displayMode: @WallpaperDisplayMode Int = WallpaperDisplayMode.CROP,
    val title: String? = null,
    val source: String? = null,
    val author: String? = null,
    val actionUri: String? = null,
) : Parcelable

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    WallpaperUpdateEventType.TIME_ELAPSED,
    WallpaperUpdateEventType.NOW_PLAYING_CHANGED,
    WallpaperUpdateEventType.CARD_FOCUSED,
    WallpaperUpdateEventType.PROGRAM_CARD_FOCUSED,
    WallpaperUpdateEventType.LAUNCHER_IDLE_MODE_CHANGED,
)
annotation class WallpaperUpdateEventType {
    companion object {
        const val TIME_ELAPSED = 1
        const val NOW_PLAYING_CHANGED = 2
        const val CARD_FOCUSED = 4
        const val PROGRAM_CARD_FOCUSED = 8
        const val LAUNCHER_IDLE_MODE_CHANGED = 16
    }
}

sealed class Event(open val eventType: Int) : Parcelable {
    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event? {
            val initialDataPosition = parcel.dataPosition()
            val eventType = parcel.readInt()
            parcel.setDataPosition(initialDataPosition)
            return when (eventType) {
                WallpaperUpdateEventType.TIME_ELAPSED -> createEventFromParcel(parcel, TimeElapsed::class.java)
                WallpaperUpdateEventType.NOW_PLAYING_CHANGED -> createEventFromParcel(parcel, NowPlayingChanged::class.java)
                WallpaperUpdateEventType.CARD_FOCUSED -> createEventFromParcel(parcel, CardFocused::class.java)
                WallpaperUpdateEventType.PROGRAM_CARD_FOCUSED -> createEventFromParcel(parcel, ProgramCardFocused::class.java)
                WallpaperUpdateEventType.LAUNCHER_IDLE_MODE_CHANGED -> createEventFromParcel(parcel, LauncherIdleModeChanged::class.java)
                else -> null
            }
        }

        override fun newArray(size: Int) = arrayOfNulls<Event?>(size)

        @Suppress("UNCHECKED_CAST")
        private fun <T> createEventFromParcel(parcel: Parcel, clazz: Class<T>): T {
            val creator = clazz.getField("CREATOR").get(null) as Parcelable.Creator<*>
            return creator.createFromParcel(parcel) as T
        }
    }

    @Parcelize
    data class TimeElapsed(
        override val eventType: Int = WallpaperUpdateEventType.TIME_ELAPSED,
    ) : Event(eventType)

    @Parcelize
    data class NowPlayingChanged(
        override val eventType: Int = WallpaperUpdateEventType.NOW_PLAYING_CHANGED,
        val isPlaying: Boolean = true,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val iconUri: String? = null,
    ) : Event(eventType)

    @Parcelize
    data class CardFocused(
        override val eventType: Int = WallpaperUpdateEventType.CARD_FOCUSED,
        val lightColor: Int = 0,
        val darkColor: Int = 0,
    ) : Event(eventType)

    @Parcelize
    data class ProgramCardFocused(
        override val eventType: Int = WallpaperUpdateEventType.PROGRAM_CARD_FOCUSED,
        val title: String? = null,
        val type: Int? = null,
        val iconUri: String? = null,
        val iconAspectRatio: Int = 0,
    ) : Event(eventType)

    @Parcelize
    data class LauncherIdleModeChanged(
        override val eventType: Int = WallpaperUpdateEventType.LAUNCHER_IDLE_MODE_CHANGED,
        val isIdle: Boolean = true,
    ) : Event(eventType)
}

object WallpaperProviderContract {
    const val ACTION_WALLPAPER_PROVIDER_UPDATED = "tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED"
    const val EXTRA_PROVIDER_ID = "tv.projectivy.plugin.extra.PROVIDER_ID"
    const val EXTRA_UPDATE_REASON = "tv.projectivy.plugin.extra.UPDATE_REASON"

    object UpdateReason {
        const val PREFS_CHANGED = 1
        const val DATA_CHANGED = 2
    }
}
