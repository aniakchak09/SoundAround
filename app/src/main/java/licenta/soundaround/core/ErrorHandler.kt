package licenta.soundaround.core

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Exception.toUserMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection."
    is SocketTimeoutException -> "Connection timed out. Try again."
    is IOException -> "Network error. Check your connection."
    else -> when {
        message?.contains("401") == true -> "Session expired. Please sign in again."
        message?.contains("403") == true -> "You don't have permission to do that."
        message?.contains("404") == true -> "Not found."
        message?.contains("500") == true -> "Server error. Try again later."
        message?.contains("offline") == true ||
        message?.contains("Unable to resolve host") == true ||
        message?.contains("Failed to connect") == true -> "No internet connection."
        else -> "Something went wrong. Please try again."
    }
}
