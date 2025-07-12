package ru.kuiva.telegramchatsarchive

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kuiva.telegramchatsarchive.data.Message
import ru.kuiva.telegramchatsarchive.service.parseTelegramHtml
import ru.kuiva.telegramchatsarchive.ui.theme.MyMessageColor
import ru.kuiva.telegramchatsarchive.ui.theme.OtherMessageColor
import ru.kuiva.telegramchatsarchive.ui.theme.TelegramArchiveTheme
import ru.kuiva.telegramchatsarchive.ui.theme.TextColor


class MainActivity : ComponentActivity() {
    val TAG = "KUIVA"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelegramArchiveTheme{
                FilePickerScreen()
            }
        }
    }
}

@Composable
fun FilePickerScreen() {
    val context = LocalContext.current
    var fileContent by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    // Лаунчер для выбора файла
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                readFileContent(context, uri)?.let { content ->
                    fileContent = content
                    fileName = getFileName(context, uri)
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                // Запускаем выбор файла (можно указать MIME-типы)
                filePickerLauncher.launch(arrayOf("*/*")) // Все типы файлов
            }
        ) {
            Text("Выбрать файл")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (fileName.isNotEmpty()) {
            ChatScreen("1", parseTelegramHtml(fileContent))
        }
    }
}
// Чтение содержимого файла
private fun readFileContent(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { reader ->
                reader.readText()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Получение имени файла
private fun getFileName(context: Context, uri: Uri): String {
    return when (uri.scheme) {
        "content" -> {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    uri.path?.substringAfterLast('/') ?: "unknown"
                }
            } ?: uri.path?.substringAfterLast('/') ?: "unknown"
        }

        else -> uri.path?.substringAfterLast('/') ?: "unknown"
    }
}


// ChatScreen.kt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    chatId: String,
    messages: List<Message>
) {

    val scrollState = rememberLazyListState()


        Scaffold { padding ->

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                reverseLayout = false
            ) {

                items(
                    count = messages.size
                ) { index ->
                    val message = messages[index]

                    MessageBubble(
                        message = message,
                        isMyMessage = message.isOutgoing,
                        modifier = Modifier
                            .fillMaxWidth()

                    )
                }
            }
        }


}
/*// ChatsScreen.kt
@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel = hiltViewModel(),
    onChatClick: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()

    LazyColumn {
        items(chats) { chat ->
            ChatListItem(
                chat = chat,
                onClick = { onChatClick(chat.id) }
            )
        }
    }
}*/

/*
// ChatListItem.kt
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар чата
        AsyncImage(
            model = chat.photoPath,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            // Название чата и время
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = chat.lastMessage?.date?.formatTime() ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Последнее сообщение
            Text(
                text = chat.lastMessage?.text ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Непрочитанные сообщения
        if (chat.unreadCount > 0) {
            Badge(
                modifier = Modifier.size(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = chat.unreadCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
*/
@Composable
fun messageShape(isMyMessage: Boolean): RoundedCornerShape {
    return if (isMyMessage) {
        // Хвостик справа
        RoundedCornerShape(
            topStart = 17.dp,
            topEnd = 2.dp,
            bottomStart = 17.dp,
            bottomEnd = 17.dp
        )
    } else {
        // Хвостик слева
        RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 17.dp,
            bottomStart = 17.dp,
            bottomEnd = 17.dp
        )
    }
}


// MessageBubble.kt
@Composable
fun MessageBubble(
    message: Message,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isMyMessage) MyMessageColor else OtherMessageColor
    val alignment = if (isMyMessage) Alignment.End else Alignment.Start
    val colorScheme = MaterialTheme.colorScheme
    val textColor = if (isMyMessage) colorScheme.onPrimaryContainer else colorScheme.onSurface
    Column(
        modifier = modifier
            .background(color = Color.Blue)
            .fillMaxWidth(0.8f)
            .wrapContentWidth(alignment) // Выравнивание по стороне
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .wrapContentWidth(alignment),
        horizontalAlignment = alignment
    ) {

        // Текст сообщения
        Surface(
            shape = messageShape(isMyMessage),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                message.text?.let { text ->
                    Text(
                        overflow = TextOverflow.Ellipsis,
                        text = text,
                        color =  textColor,
                        style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 0.dp)
                    )
                }
                message.voiceMessage?.let { voice ->
                    Text(
                        text = voice.path,
                        color =  textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                }

                Text(
                    text = message.date.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }

    }
}