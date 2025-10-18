![Maven Central Version](https://img.shields.io/maven-central/v/dev.limebeck.libs/mattermost-client)

# Mattermost Client Library

Multiplatform Kotlin library for working with the Mattermost API. It helps you build bots and integrations that can receive direct messages, detect new chats, and send messages back to channels/users.

## Supported platforms

- JVM – for server applications and Android
- JavaScript – for the browser and Node.js
- Wasm – for the browser and Node.js
- Native (Linux) – for native applications on Linux

## Module
- [mattermost-client](mattermost-client) — core client with a simple high-level API.

## Add as a dependency
Coordinates (published to Maven Central):
- Group: dev.limebeck.libs
- Artifact: mattermost-client
- Version: 1.0.0

Gradle Kotlin DSL (build.gradle.kts):
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.limebeck.libs:mattermost-client:1.0.0")
}
```

Gradle Groovy DSL (build.gradle):
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'dev.limebeck.libs:mattermost-client:1.0.0'
}
```

Maven (pom.xml):
```xml
<dependencies>
  <dependency>
    <groupId>dev.limebeck.libs</groupId>
    <artifactId>mattermost-client</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

Note: This is a Kotlin Multiplatform library. Gradle will pick the right variant for your target (JVM, JS, Linux) automatically.

## Quick start

- Create a personal access token in your Mattermost server.
- Instantiate a client and send a message.

```kotlin
suspend fun main() {
    val baseUrl = "https://your.mattermost.server"
    val token = "MM_PERSONAL_ACCESS_TOKEN"
    val client = MattermostClientImpl(baseUrl, token)

    client.sendMessage(ChannelId("channel-id"), "Hello from Kotlin!")
}
```

## Example: Processing a user request with attachments and an LLM

Below is a real-world example that:
- Receive the incoming user request.
- Builds a message for an LLM.
- Sends the LLM response back to Mattermost, reporting token usage.

```kotlin
mattermostClient.receiveDirectMessages().collect { message ->
    logger.info("<173c4c43> Processing client request ${message.userName}")

    val response = gptClient.getCompletion(message.text)

    response.onSuccess { r ->
        mattermostClient.sendMessage(message.channelId, r.text)
    }.onFailure { t ->
        val requestUuid = Uuid.random().toString()
        logger.error("<ea88cf0c> An error occurred while processing the request $requestUuid", t)
        mattermostClient.sendMessage(
            message.channelId,
            "An error occurred while processing the request. Error code $requestUuid.\n${t.message}",
        )
    }
}
```

Notes:
- mattermostClient.sendMessage(ChannelId, String) is provided by this library.
- DirectMessage objects you receive from Mattermost include attachments with id, name, mimeType, and raw bytes (data).
- For long responses, consider splitting large Markdown into chunks before sending. This project already includes a helper splitMarkdown in mattermost-client/utils.

## License
This project is distributed under the terms specified in the LICENCE file.
