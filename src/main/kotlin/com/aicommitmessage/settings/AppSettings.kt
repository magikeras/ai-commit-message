package com.aicommitmessage.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(
    name = "com.aicommitmessage.settings.AppSettings",
    storages = [Storage("AiCommitMessage.xml")]
)
class AppSettings : PersistentStateComponent<AppSettings.State> {

    class State {
        var model: String = "claude-sonnet-4-6"
        var locale: String = "en"
        var customPrompt: String = ""
        var messageDepth: String = "short"
        var skipTag: String = "none"
        var askSkipTag: Boolean = false
        var defaultSkipTagEnabled: Boolean = false
    }

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var model: String
        get() = myState.model
        set(value) { myState.model = value }

    var locale: String
        get() = myState.locale
        set(value) { myState.locale = value }

    var customPrompt: String
        get() = myState.customPrompt
        set(value) { myState.customPrompt = value }

    var messageDepth: String
        get() = myState.messageDepth
        set(value) { myState.messageDepth = value }

    var skipTag: String
        get() = myState.skipTag
        set(value) { myState.skipTag = value }

    var askSkipTag: Boolean
        get() = myState.askSkipTag
        set(value) { myState.askSkipTag = value }

    var defaultSkipTagEnabled: Boolean
        get() = myState.defaultSkipTagEnabled
        set(value) { myState.defaultSkipTagEnabled = value }

    var apiKey: String
        get() {
            val attributes = createCredentialAttributes()
            return PasswordSafe.instance.getPassword(attributes).orEmpty()
        }
        set(value) {
            val attributes = createCredentialAttributes()
            PasswordSafe.instance.setPassword(attributes, value)
        }

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName("AiCommitMessage", "anthropic-api-key")
        )
    }

    companion object {
        val instance: AppSettings
            get() = ApplicationManager.getApplication().getService(AppSettings::class.java)
    }
}
