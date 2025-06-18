package ru.kuiva.telegramchatsarchive.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val chatId: String,
    val title: String,
    val lastMessageTime: String?,
    val avatarPath: String?
)

@Entity(tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Chat::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")])
data class Message(
    // Основные поля
    @PrimaryKey val id: String,
    val chatId: String,
    val isOutgoing: Boolean,
    val date: String,
    val from: String,
    val text: String?,
    val isService: Boolean = false,

    // Медиа-вложения
    val photo: PhotoAttachment? = null,
    val video: VideoAttachment? = null,
    val voiceMessage: VoiceAttachment? = null,
    val sticker: StickerAttachment? = null,
    val animatedSticker: AnimatedStickerAttachment? = null,
    val document: DocumentAttachment? = null,

    // Дополнительные данные
    val replyToMessageId: String? = null,
    val forwardedFrom: String? = null,
    val reactions: List<Reaction>? = null,
    val edited: Boolean = false,
    val editedDate: String? = null
)

// Модели для вложений
data class PhotoAttachment(
    val path: String,
    val thumbPath: String?,
    val width: Int?,
    val height: Int?,
    val fileSize: Int?
)

data class VideoAttachment(
    val path: String,
    val thumbPath: String?,
    val duration: Int?,
    val width: Int?,
    val height: Int?,
    val fileSize: Int?
)

data class VoiceAttachment(
    val path: String,
    val duration: Int?,
    val fileSize: Int?
)

data class StickerAttachment(
    val path: String,
    val emoji: String?
)

data class AnimatedStickerAttachment(
    val path: String,
    val emoji: String?
)

data class DocumentAttachment(
    val path: String,
    val fileSize: Int?,
    val mimeType: String?
)

data class Reaction(
    val emoji: String,
    val count: Int,
    val from: String? = null
)