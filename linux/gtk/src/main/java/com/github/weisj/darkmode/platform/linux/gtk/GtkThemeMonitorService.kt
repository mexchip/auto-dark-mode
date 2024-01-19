/*
 * MIT License
 *
 * Copyright (c) 2020 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.github.weisj.darkmode.platform.linux.gtk

import com.github.weisj.darkmode.platform.Compatibility
import com.github.weisj.darkmode.platform.NativePointer
import com.github.weisj.darkmode.platform.PluginLogger
import com.github.weisj.darkmode.platform.ThemeMonitorService
import com.github.weisj.darkmode.platform.linux.gtk.GtkVariants.guessFrom

enum class SignalType(internal val id : Int) {
    GTK(0),
    GIO(1)
}

class GtkThemeMonitorService(
    val signalType: SignalType
) : ThemeMonitorService {
    companion object {
        val LOGGER = PluginLogger<GtkThemeMonitorService>()
    }

    override val isDarkThemeEnabled: Boolean
        get() {
            val currentTheme = currentGtkTheme
            LOGGER.info("Checking whether dark mode is enabled. The current theme is '$currentTheme'")
            return if (GtkSettings.guessLightAndDarkThemes) {
                currentTheme == guessFrom(currentTheme)[GtkVariants.Variant.Night]
            } else {
                GtkSettings.darkGtkTheme.name == currentTheme
            }
        }
    override val isHighContrastEnabled: Boolean
        get() {
            if (GtkSettings.guessLightAndDarkThemes) return false
            val currentTheme = currentGtkTheme
            LOGGER.info("Checking whether high contrast mode is enabled. The current theme is '$currentTheme'")
            return GtkSettings.highContrastGtkTheme.name == currentTheme
        }
    override val compatibility: Compatibility = if (GtkLibrary.get().isLoaded) {
        Compatibility(true, "")
    } else {
        Compatibility(false, "Desktop environment isn't one of GNOME, Xfce, I3")
    }
    val currentGtkTheme: String
        get() = GtkNative.getCurrentTheme(signalType.id)

    override fun createEventHandler(callback: () -> Unit): NativePointer? {
        return NativePointer(GtkNative.createEventHandler(signalType.id) {
            LOGGER.info("Received notification")
            callback()
        })
    }

    override fun deleteEventHandler(eventHandle: NativePointer) {
        GtkNative.deleteEventHandler(eventHandle.pointer)
    }

    override fun install() {
        GtkNative.init()
    }
}
