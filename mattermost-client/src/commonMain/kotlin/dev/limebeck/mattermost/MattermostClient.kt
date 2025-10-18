package dev.limebeck.mattermost

import dev.limebeck.mattermost.types.*
import kotlinx.coroutines.flow.Flow

interface MattermostClient {
    suspend fun receiveDirectMessages(): Flow<DirectMessage>
    suspend fun receiveNewChatStarted(): Flow<NewChatStartedEvent>
    suspend fun sendMessage(channelId: ChannelId, message: String)
    suspend fun isMemberOfTeam(userId: UserId, teamId: TeamId): Boolean
}
