package com.aicommitmessage.util

object BranchTicket {

    private val TICKET_REGEX = Regex("""(?<![A-Za-z0-9])CL-\d+""", RegexOption.IGNORE_CASE)

    fun extractTicketId(branchName: String): String? =
        TICKET_REGEX.find(branchName)?.value?.uppercase()

    fun appendTicket(message: String, ticketId: String): String {
        val tag = "[$ticketId]"
        if (message.contains(tag, ignoreCase = true)) return message
        return "${message.trimEnd()} $tag"
    }
}
