package dev.limebeck.mattermost

import dev.limebeck.libs.logger.logger
import dev.limebeck.mattermost.types.internal.FileInfo
import dev.limebeck.mattermost.types.internal.InternalEvent
import dev.limebeck.mattermost.types.internal.Post
import dev.limebeck.mattermost.types.internal.PostMetadata
import dev.limebeck.mattermost.types.internal.PostToSend
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import dev.limebeck.mattermost.utils.splitMarkdown
import kotlin.time.Duration.Companion.seconds

class MattermostClientImpl(
    baseUrl: String,
    private val apiToken: String,
    private val chunkSize: Int = 16383,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : MattermostClient {
    companion object {
        private val logger = MattermostClientImpl::class.logger()

        private const val API_PATH = "/api/v4"
    }

    private val baseUrl = baseUrl.removeSuffix("/")

    private val jsonMapper = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    //TODO: Pass engine, client or client factory from outside
    private val client = HttpClient(CIO) {
        defaultRequest {
            contentType(ContentType.Application.Json)
            bearerAuth(apiToken)
        }

        install(WebSockets) {
            pingInterval = 20.seconds
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    MattermostClientImpl.logger.debug { message }
                }
            }
            level = LogLevel.ALL
            sanitizeHeader { it == "Authorization" }
        }

        install(ContentNegotiation) {
            json(jsonMapper)
        }
    }

    //TODO: Refactor this: pass flow into `listen` function
    private val internalEventsFlow = MutableSharedFlow<InternalEvent>()

    //TODO: Run launch into `listen`, make accessible from outside
    private fun launch() {
        val url = baseUrl
            .replace("http://", "ws://")
            .replace("https://", "wss://") + API_PATH + "/websocket"

        scope.launch {
            while (isActive) {
                try {
                    client.webSocket(url) {
                        logger.info { "<3eaf6bd6> WebSocket connection established: $url" }
                        for (frame in incoming) {
                            val message = frame as? Frame.Text
                            if (message != null) {
                                val text = message.readText()
                                logger.debug { "<c362de7a> Received message from WebSocket: $text" }
                                val event = jsonMapper.decodeFromString<InternalEvent>(text)
                                internalEventsFlow.emit(event)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "WebSocket error:" }
                    delay(3000) // Pause before reconnection
                }
            }
        }
    }

    init {
        launch()
    }

    override suspend fun receiveDirectMessages(): Flow<DirectMessage> =
        internalEventsFlow
            .filter { it.event == "posted" }
            .map { it to jsonMapper.decodeFromString<Post>(it.data.jsonObject["post"]?.jsonPrimitive?.content!!) }
            .filter { (event, post) ->
                post.props?.get("from_bot")?.jsonPrimitive?.booleanOrNull != true
                        && event.data.jsonObject["channel_type"]?.jsonPrimitive?.content == "D"
            }.map { (event, post) ->
                val fileMetadataById =
                    buildMap {
                        post.metadata?.files?.forEach { file ->
                            put(file.id, file)
                        }
                        post.fileIds?.forEach { fileId ->
                            put(fileId, PostMetadata.FileMetadata(id = fileId))
                        }
                    }

                val attachments =
                    fileMetadataById.values.mapNotNull { file ->
                        runCatching {
                            val info =
                                if (file.name == null || file.mimeType == null) {
                                    client.get("$baseUrl$API_PATH/files/${file.id}/info").body<FileInfo>()
                                } else {
                                    null
                                }

                            val bytes = client.get("$baseUrl$API_PATH/files/${file.id}").body<ByteArray>()

                            Attachment(
                                id = file.id,
                                name = file.name ?: info?.name ?: file.id,
                                mimeType = file.mimeType ?: info?.mimeType ?: "application/octet-stream",
                                data = bytes,
                            )
                        }.onFailure {
                            logger.error(it) { "<download-file-error> Error downloading file ${file.id}" }
                        }.getOrNull()
                    }

                DirectMessage(
                    channelId = post.channelId,
                    userId = post.userId,
                    userName = event.data.jsonObject["sender_name"]?.jsonPrimitive?.content ?: "unknown",
                    text = post.message,
                    attachments = attachments,
                )
            }.onEach { logger.info { "<eb86d64d> Message from user ${it.userName}: ${it.text.take(200)}" } }

    override suspend fun receiveNewChatStarted(): Flow<NewChatStartedEvent> = internalEventsFlow
        .filter { it.event == "direct_added" }
        .map { event -> event to event.data.jsonObject["creator_id"]?.jsonPrimitive?.content?.let { UserId(it) } }
        .filter { (event, userId) -> userId != null && event.broadcast.channelId != null }
        .map { (event, userId) ->
            NewChatStartedEvent(
                channelId = event.broadcast.channelId!!,
                userId = userId!!,
            )
        }.onEach { logger.info { "<eeb2bb55> New chat with user ID ${it.userId}" } }

    override suspend fun sendMessage(channelId: ChannelId, message: String) {
        logger.info { "<3c60bc9a> Sending message to channel $channelId: $message" }

        val messageChunks = splitMarkdown(message, chunkSize)

        try {
            for (chunk in messageChunks) {
                repeatOnException(10) {
                    val result = client.post("$baseUrl$API_PATH/posts") {
                        setBody(PostToSend(channelId, chunk, JsonObject(mapOf("from_bot" to JsonPrimitive(true)))))
                    }

                    if (result.status != HttpStatusCode.Created) {
                        throw RuntimeException("<b326ae01> Error sending message to Mattermost. status = ${result.status}. Response: ${result.bodyAsText()}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "<9ffa0dd3> Error while sending message to Mattermost:" }
        }
    }

    private suspend inline fun repeatOnException(retries: Int, crossinline block: suspend () -> Unit) {
        repeat(retries) { i ->
            try {
                block()
                return
            } catch (e: Exception) {
                logger.error(e) { "<a3c1e770> Got error #$i" }
            }
        }
    }

    override suspend fun isMemberOfTeam(userId: UserId, teamId: TeamId): Boolean {
        val result = client.get("$baseUrl$API_PATH/teams/${teamId.value}/members/${userId.value}")
        return result.status == HttpStatusCode.OK
    }
}
