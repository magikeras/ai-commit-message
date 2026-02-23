package com.aicommitmessage.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager

object DiffUtil {

    private const val MAX_DIFF_LENGTH = 10_000

    fun getStagedDiff(project: Project): String {
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges

        if (changes.isEmpty()) {
            return ""
        }

        val sb = StringBuilder()
        for (change in changes) {
            val beforeRevision = change.beforeRevision
            val afterRevision = change.afterRevision

            val filePath = (afterRevision?.file ?: beforeRevision?.file) ?: continue
            val relativePath = filePath.path

            sb.appendLine("--- a/$relativePath")
            sb.appendLine("+++ b/$relativePath")

            val beforeContent = beforeRevision?.content.orEmpty()
            val afterContent = afterRevision?.content.orEmpty()

            if (beforeContent.isEmpty() && afterContent.isNotEmpty()) {
                sb.appendLine("(new file)")
                val lines = afterContent.lines().take(100)
                for (line in lines) {
                    sb.appendLine("+$line")
                }
            } else if (beforeContent.isNotEmpty() && afterContent.isEmpty()) {
                sb.appendLine("(deleted file)")
                val lines = beforeContent.lines().take(50)
                for (line in lines) {
                    sb.appendLine("-$line")
                }
            } else {
                val beforeLines = beforeContent.lines()
                val afterLines = afterContent.lines()
                appendSimpleDiff(sb, beforeLines, afterLines)
            }

            sb.appendLine()

            if (sb.length > MAX_DIFF_LENGTH) {
                sb.appendLine("... (diff truncated)")
                break
            }
        }

        return sb.toString().take(MAX_DIFF_LENGTH)
    }

    private fun appendSimpleDiff(sb: StringBuilder, before: List<String>, after: List<String>) {
        val beforeSet = before.toSet()
        val afterSet = after.toSet()

        val removed = before.filter { it !in afterSet }
        val added = after.filter { it !in beforeSet }

        for (line in removed.take(50)) {
            sb.appendLine("-$line")
        }
        for (line in added.take(50)) {
            sb.appendLine("+$line")
        }
    }
}
