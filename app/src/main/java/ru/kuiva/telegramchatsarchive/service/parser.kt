package ru.kuiva.telegramchatsarchive.service


import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.kuiva.telegramchatsarchive.data.AnimatedStickerAttachment
import ru.kuiva.telegramchatsarchive.data.DocumentAttachment
import ru.kuiva.telegramchatsarchive.data.Message
import ru.kuiva.telegramchatsarchive.data.PhotoAttachment
import ru.kuiva.telegramchatsarchive.data.Reaction
import ru.kuiva.telegramchatsarchive.data.StickerAttachment
import ru.kuiva.telegramchatsarchive.data.VideoAttachment
import ru.kuiva.telegramchatsarchive.data.VoiceAttachment
import java.io.File

fun parseTelegramHtml(file: String): List<Message> {
    val doc = Jsoup.parse(file)
    val messages = mutableListOf<Message>()

    doc.select("div.message").forEach { msg ->
        val isService = msg.hasClass("service")
        val id = msg.attr("id")

        if (isService) {
            // Парсим сервисные сообщения (даты)
            messages.add(
                Message(
                    id = id,
                    date = "",
                    from = "",
                    text = msg.select("div.body.details").text(),
                    isService = true,
                    isOutgoing = false
                )
            )
        } else {
            // Парсим обычные сообщения
            val date = msg.select("div.date").attr("title").ifEmpty {
                msg.select("div.date").text()
            }
            val from = msg.select("div.from_name").text().ifEmpty { messages.getOrNull(messages.lastIndex)?.from?:" " }

            // Парсим текст (может быть сложной структурой)
            val text = parseMessageText(msg)

            // Парсим вложения
            val photo = parsePhotoAttachment(msg)
            val video = parseVideoAttachment(msg)
            val voice = parseVoiceAttachment(msg)
            val sticker = parseStickerAttachment(msg)
            val animatedSticker = parseAnimatedSticker(msg)
            val document = parseDocumentAttachment(msg)

            // Парсим дополнительные данные
            val replyTo = msg.select("a[onclick^=return GoToMessage]")
                .firstOrNull()?.attr("onclick")
                ?.removePrefix("return GoToMessage(")
                ?.removeSuffix(")")

            val reactions = parseReactions(msg)
            val edited = msg.select("div.edited").isNotEmpty()
            val editedDate = if (edited) msg.select("div.edited").attr("title") else null

            messages.add(
                Message(
                    id = id,
                    isOutgoing = from=="Олег Заостровцев",
                    date = date,
                    from = from,
                    text = text,
                    isService = false,
                    photo = photo,
                    video = video,
                    voiceMessage = voice,
                    sticker = sticker,
                    animatedSticker = animatedSticker,
                    document = document,
                    replyToMessageId = replyTo,
                    reactions = reactions,
                    edited = edited,
                    editedDate = editedDate
                )
            )
        }
    }

    return messages
}

// Вспомогательные функции парсинга
private fun parseMessageText(msg: Element): String? {
    return when {
        msg.select("div.text").isNotEmpty() -> msg.select("div.text").text()
        msg.select("div.media_wrap").isNotEmpty() -> null // Медиа-сообщение без текста
        else -> null
    }
}

private fun parsePhotoAttachment(msg: Element): PhotoAttachment? {
    val photoWrap = msg.select("a.photo_wrap").firstOrNull() ?: return null
    return PhotoAttachment(
        path = photoWrap.attr("href"),
        thumbPath = photoWrap.select("img.photo").attr("src"),
        width = photoWrap.select("img.photo").attr("width").toIntOrNull(),
        height = photoWrap.select("img.photo").attr("height").toIntOrNull(),
        fileSize = null // В HTML экспорте нет информации о размере
    )
}

private fun parseVideoAttachment(msg: Element): VideoAttachment? {
    val videoWrap = msg.select("a.video_file_wrap").firstOrNull() ?: return null
    return VideoAttachment(
        path = videoWrap.attr("href"),
        thumbPath = videoWrap.select("img.video_file").attr("src"),
        duration = videoWrap.select("div.video_duration").text()
            .removePrefix("00:").toIntOrNull(),
        width = videoWrap.select("img.video_file").attr("width").toIntOrNull(),
        height = videoWrap.select("img.video_file").attr("height").toIntOrNull(),
        fileSize = null
    )
}

private fun parseVoiceAttachment(msg: Element): VoiceAttachment? {
    val voiceWrap = msg.select("a.media_voice_message").firstOrNull() ?: return null
    return VoiceAttachment(
        path = voiceWrap.attr("href"),
        duration = voiceWrap.select("div.status.details").text()
            .removePrefix("00:").toIntOrNull(),
        fileSize = null
    )
}

private fun parseStickerAttachment(msg: Element): StickerAttachment? {
    val stickerLink = msg.select("a[href^=stickers/][href$=.webp]").firstOrNull() ?: return null
    return StickerAttachment(
        path = stickerLink.attr("href"),
        emoji = stickerLink.text()
    )
}

private fun parseAnimatedSticker(msg: Element): AnimatedStickerAttachment? {
    val stickerLink = msg.select("a[href^=stickers/][href$=.tgs]").firstOrNull() ?: return null
    return AnimatedStickerAttachment(
        path = stickerLink.attr("href"),
        emoji = stickerLink.text()
    )
}

private fun parseDocumentAttachment(msg: Element): DocumentAttachment? {
    val docWrap = msg.select("a.media_document").firstOrNull() ?: return null
    return DocumentAttachment(
        path = docWrap.attr("href"),
        fileSize = docWrap.select("div.file_size").text()
            .removeSuffix(" KB").toIntOrNull()?.times(1024),
        mimeType = docWrap.select("div.mime_type").text()
    )
}

private fun parseReactions(msg: Element): List<Reaction>? {
    val reactions = msg.select("span.reaction").takeIf { it.isNotEmpty() } ?: return null
    return reactions.map { reaction ->
        Reaction(
            emoji = reaction.select("span.emoji").text(),
            count = reaction.select("span.count").text().toIntOrNull() ?: 1,
            from = reaction.select("div.userpic").attr("title")
        )
    }
}