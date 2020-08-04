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
#include "com_github_weisj_darkmode_platform_linux_gnome_GnomeNative.h"
#include "GioUtils.hpp"

#include <string>
#include <glibmm-2.4/glibmm.h>
#include <giomm-2.4/giomm.h>

constexpr auto SETTINGS_SCHEMA_NAME = "org.gnome.desktop.interface";
constexpr auto THEME_NAME_KEY = "gtk-theme";
Glib::RefPtr<Gio::Settings> settings;

JNIEXPORT jstring JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_getCurrentTheme(JNIEnv *env, jclass) {
    return env->NewStringUTF(settings->get_string(THEME_NAME_KEY).c_str());
}

struct EventHandler {
    JavaVM *jvm;
    JNIEnv *env;
    jobject callback;
    sigc::connection settingsChangedSignalConnection;

    void settingChanged(const Glib::ustring &name) {
        runCallBack();
    }

    void runCallBack() {
        bool detachNecessary = false;
        int getEnvStat = jvm->GetEnv((void**) &env, JNI_VERSION_1_6);
        if (getEnvStat == JNI_EDETACHED) {
            detachNecessary = true;
            if (jvm->AttachCurrentThread((void**) &env, NULL) != 0) return;
        } else if (getEnvStat == JNI_EVERSION) {
            return;
        }

        jclass runnableClass = env->GetObjectClass(callback);
        jmethodID runMethodId = env->GetMethodID(runnableClass, "run", "()V");
        if (runMethodId) {
            env->CallVoidMethod(callback, runMethodId);
        }
        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
        }
        if (detachNecessary) jvm->DetachCurrentThread();
    }

    void stop() {
        settingsChangedSignalConnection.disconnect();
    }

    EventHandler(JavaVM *jvm_, jobject callback_) {
        jvm = jvm_;
        callback = callback_;
        settingsChangedSignalConnection = settings->signal_changed(THEME_NAME_KEY).connect(
                sigc::mem_fun(this, &EventHandler::settingChanged));
    }
};

JNIEXPORT jlong JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_createEventHandler(JNIEnv *env, jclass obj, jobject callback) {
    JavaVM *jvm;
    if (env->GetJavaVM(&jvm) == JNI_OK) {
        jobject callbackRef = env->NewGlobalRef(callback);
        EventHandler* eventHandler = new EventHandler(jvm, callbackRef);
        return reinterpret_cast<jlong>(eventHandler);
    }
    return (jlong) 0;
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_deleteEventHandler(JNIEnv *env, jclass obj, jlong eventHandler)
{
    EventHandler *handler = reinterpret_cast<EventHandler *>(eventHandler);
    if (handler) {
        env->DeleteGlobalRef(handler->callback);
        handler->stop();
        delete handler;
    }
}

JNIEXPORT void JNICALL
Java_com_github_weisj_darkmode_platform_linux_gnome_GnomeNative_init(JNIEnv *env, jclass obj) {
    ensure_gio_init();
    settings = Gio::Settings::create(SETTINGS_SCHEMA_NAME);
}
