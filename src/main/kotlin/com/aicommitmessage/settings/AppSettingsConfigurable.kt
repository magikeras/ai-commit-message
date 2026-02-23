package com.aicommitmessage.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class AppSettingsConfigurable : Configurable {

    private var component: AppSettingsComponent? = null

    override fun getDisplayName(): String = "AI Commit Message"

    override fun createComponent(): JComponent {
        component = AppSettingsComponent()
        return component!!.panel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return component?.preferredFocusedComponent
    }

    override fun isModified(): Boolean {
        val settings = AppSettings.instance
        val comp = component ?: return false
        return comp.apiKey != settings.apiKey ||
                comp.model != settings.model ||
                comp.locale != settings.locale ||
                comp.customPrompt != settings.customPrompt
    }

    override fun apply() {
        val settings = AppSettings.instance
        val comp = component ?: return
        settings.apiKey = comp.apiKey
        settings.model = comp.model
        settings.locale = comp.locale
        settings.customPrompt = comp.customPrompt
    }

    override fun reset() {
        val settings = AppSettings.instance
        val comp = component ?: return
        comp.apiKey = settings.apiKey
        comp.model = settings.model
        comp.locale = settings.locale
        comp.customPrompt = settings.customPrompt
    }

    override fun disposeUIResources() {
        component = null
    }
}
