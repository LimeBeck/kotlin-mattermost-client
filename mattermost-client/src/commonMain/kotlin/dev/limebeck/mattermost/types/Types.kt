package dev.limebeck.mattermost.types

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

data class DirectMessage(
    val channelId: ChannelId,
    val userId: UserId,
    val text: String,
    val userName: String,
    val attachments: List<Attachment> = emptyList(),
)

data class Attachment(
    val id: String,
    val name: String,
    val mimeType: String,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Attachment

        if (id != other.id) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class NewChatStartedEvent(
    val userId: UserId,
    val channelId: ChannelId,
)

@Serializable
@JvmInline
value class ChannelId(val value: String)

@Serializable
@JvmInline
value class UserId(val value: String)

@Serializable
@JvmInline
value class TeamId(val value: String)
