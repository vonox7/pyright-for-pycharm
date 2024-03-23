package com.insyncwithfoo.pyright.runner

import com.insyncwithfoo.pyright.message
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem


private fun NotificationGroup.createErrorNotification(title: String, content: String) =
    createNotification(title, content, NotificationType.ERROR)


private class OpenFileAction(text: String, private val path: String) : NotificationAction(text) {
    
    override fun actionPerformed(event: AnActionEvent, notification: Notification) {
        val project = event.project ?: return somethingIsWrong()
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        val fileSystem = LocalFileSystem.getInstance()
        val virtualFile = fileSystem.findFileByPath(path) ?: return somethingIsWrong()
        
        fileEditorManager.openFileEditor(OpenFileDescriptor(project, virtualFile), true)
    }
    
    private fun somethingIsWrong() {
        val text = message("notifications.error.action.openConfigurationFile.error.body", path)
        @Suppress("DialogTitleCapitalization")
        val title = message("notifications.error.action.openConfigurationFile.error.title")
        
        Messages.showErrorDialog(text, title)
    }
    
}


internal sealed class PyrightException(
    val stdout: String,
    val stderr: String,
    message: String
) : Exception(message) {
    abstract fun createNotification(group: NotificationGroup): Notification
}


internal class FatalException(
    stdout: String,
    stderr: String,
    message: String = "Pyright reported a fatal error"
) : PyrightException(stdout, stderr, message) {
    
    override fun createNotification(group: NotificationGroup): Notification {
        val title = message("notifications.error.fatal.title")
        val content = message("notifications.error.fatal.body")
        
        return group.createErrorNotification(title, content).apply {
            val text = message("notifications.error.action.openPyrightIssueTracker")
            addAction(BrowseNotificationAction(text, PYRIGHT_ISSUE_TRACKER_LINK))
        }
    }
    
    companion object {
        private const val PYRIGHT_ISSUE_TRACKER_LINK =
            "https://github.com/microsoft/pyright/issues"
    }
    
}


internal class InvalidConfigurationsException(
    stdout: String,
    stderr: String,
    message: String = "Pyright cannot parse configuration file"
) : PyrightException(stdout, stderr, message) {
    
    override fun createNotification(group: NotificationGroup): Notification {
        // Greedy is necessary since a Linux path may contain double quotes:
        //   Config file "/b"az/pyrightconfig.json" could not be parsed.
        val quoted = "\"(.*)\"".toRegex()
        
        val configurationFilePath = quoted.find(stderr)!!.groups[1]!!.value
        
        val title = message("notifications.error.invalidConfigurations.title")
        val content = message("notifications.error.invalidConfigurations.body", configurationFilePath)
        
        return group.createErrorNotification(title, content).apply {
            val text = message("notifications.error.action.openConfigurationFile")
            addAction(OpenFileAction(text, configurationFilePath))
        }
    }
    
}


internal class InvalidParametersException(
    stdout: String,
    stderr: String,
    message: String = "Pyright does not recognize CLI options"
) : PyrightException(stdout, stderr, message) {
    
    override fun createNotification(group: NotificationGroup): Notification {
        val title = message("notifications.error.invalidCliOptions.title")
        val content = message("notifications.error.invalidCliOptions.body")
        
        return group.createErrorNotification(title, content).apply {
            val text = message("notifications.error.action.openPluginIssueTracker")
            addAction(BrowseNotificationAction(text, PLUGIN_ISSUE_TRACKER_LINK))
        }
    }
    
    companion object {
        private const val PLUGIN_ISSUE_TRACKER_LINK =
            "https://github.com/InSyncWithFoo/pyright-for-pycharm/issues"
    }
    
}
