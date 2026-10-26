package tv.projectivy.plugin.wallpaperprovider.api

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.parcelize.Parcelize

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
            val creatorField = clazz.getField("CREATOR")
            val creator = creatorField.get(null) as Parcelable.Creator<*>
            return creator.createFromParcel(parcel) as T
        }
    }

    @Parcelize
    data class TimeElapsed(override val eventType: Int = WallpaperUpdateEventType.TIME_ELAPSED) : Event(eventType)

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
