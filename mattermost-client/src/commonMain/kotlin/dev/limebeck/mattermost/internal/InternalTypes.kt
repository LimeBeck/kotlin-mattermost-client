package dev.limebeck.mattermost.internal

import dev.limebeck.mattermost.ChannelId
import dev.limebeck.mattermost.TeamId
import dev.limebeck.mattermost.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class InternalEvent(
    val event: String,
    val data: JsonElement,
    val broadcast: Broadcast,
    val seq: Long
)

@Serializable
data class Broadcast(
    @SerialName("user_id") val userId: UserId?,
    @SerialName("channel_id") val channelId: ChannelId?,
    @SerialName("team_id") val teamId: TeamId?,
    @SerialName("connection_id") val connectionId: String?,
    @SerialName("omit_connection_id") val omitConnectionId: String?,
)

@Serializable
data class Post(
    @SerialName("channel_id") val channelId: ChannelId,
    val message: String,
    @SerialName("user_id") val userId: UserId,
    val props: JsonObject? = null,
    @SerialName("file_ids") val fileIds: List<String>? = null,
    val metadata: PostMetadata? = null,
)

@Serializable
data class PostMetadata(
    val files: List<FileMetadata> = emptyList(),
) {
    @Serializable
    data class FileMetadata(
        val id: String,
        val name: String? = null,
        @SerialName("mime_type") val mimeType: String? = null,
    )
}

@Serializable
data class FileInfo(
    val id: String,
    val name: String,
    @SerialName("mime_type") val mimeType: String,
)

@Serializable
data class PostToSend(
    @SerialName("channel_id") val channelId: ChannelId,
    val message: String,
    val props: JsonObject
)