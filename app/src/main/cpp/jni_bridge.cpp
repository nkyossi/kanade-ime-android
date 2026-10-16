// JNI bridge between the conversion engine (core/) and NativeEngine.kt.
//
// The engine calls back the Kotlin NativeEngine.Client through JavaClient,
// which maps the IBus-like Client interface onto InputConnection:
//   get_surrounding_text()    -> Client.textBeforeCursor()
//   delete_surrounding_text() -> Client.deleteBeforeCursor(utf16Length)
//   commit_text()             -> Client.commitText()
//   update_preedit()          -> Client.updatePreedit()
//   update_lookup_table()     -> Client.updateCandidates()
//
// All the calls except nativeCreate() must come from one thread (the main
// thread of the input method service).

#include <jni.h>

#include <android/log.h>

#include <filesystem>
#include <memory>
#include <string>
#include <vector>

#include "Engine.h"
#include "Log.h"
#include "Settings.h"

using namespace hiragana;
namespace fs = std::filesystem;

namespace {

const char* TAG = "HiraganaIME";

ustring from_jstring(JNIEnv* env, jstring s) {
    if (!s) return ustring();
    const jchar* chars = env->GetStringChars(s, nullptr);
    jsize n = env->GetStringLength(s);
    ustring out = from_utf16(std::u16string_view(reinterpret_cast<const char16_t*>(chars), static_cast<size_t>(n)));
    env->ReleaseStringChars(s, chars);
    return out;
}

jstring to_jstring(JNIEnv* env, const ustring& s) {
    std::u16string u = to_utf16(s);
    return env->NewString(reinterpret_cast<const jchar*>(u.data()), static_cast<jsize>(u.size()));
}

std::string utf8_of(JNIEnv* env, jstring s) {
    if (!s) return std::string();
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

class JavaClient : public Client {
public:
    JNIEnv* env = nullptr;
    jobject client = nullptr;  // global reference to NativeEngine.Client

    ~JavaClient() override { set(nullptr); }

    void set(jobject c) {
        if (client && env) env->DeleteGlobalRef(client);
        client = c && env ? env->NewGlobalRef(c) : nullptr;
        cache_.clear();
    }

    void get_surrounding_text(ustring& text, index_t& pos) override {
        text.clear();
        pos = 0;
        if (!ready()) return;
        jstring s = static_cast<jstring>(env->CallObjectMethod(client, method("textBeforeCursor", "()Ljava/lang/String;")));
        if (check()) return;
        text = from_jstring(env, s);
        if (s) env->DeleteLocalRef(s);
        pos = len(text);
        cache_ = text;
    }

    void delete_surrounding_text(index_t size) override {
        if (!ready() || size <= 0) return;
        // The engine counts code points; InputConnection counts UTF-16 units.
        index_t n = std::min<index_t>(size, len(cache_));
        ustring deleted = slice_from(cache_, len(cache_) - n);
        jint units = static_cast<jint>(utf16_length(deleted) + (size - n));
        cache_ = slice_to(cache_, len(cache_) - n);
        env->CallVoidMethod(client, method("deleteBeforeCursor", "(I)V"), units);
        check();
    }

    void commit_text(const ustring& text) override {
        if (!ready()) return;
        cache_ += text;
        jstring s = to_jstring(env, text);
        env->CallVoidMethod(client, method("commitText", "(Ljava/lang/String;)V"), s);
        env->DeleteLocalRef(s);
        check();
    }

    void update_preedit(const Preedit& preedit) override {
        if (!ready()) return;
        // [start, end, style] in UTF-16 units for each segment
        std::vector<jint> spans;
        jint offset = 0;
        for (const auto& seg : preedit.segments) {
            jint n = static_cast<jint>(utf16_length(seg.text));
            spans.insert(spans.end(), {offset, offset + n, static_cast<jint>(seg.style)});
            offset += n;
        }
        jstring s = to_jstring(env, preedit.text());
        jintArray a = env->NewIntArray(static_cast<jsize>(spans.size()));
        env->SetIntArrayRegion(a, 0, static_cast<jsize>(spans.size()), spans.data());
        env->CallVoidMethod(client, method("updatePreedit", "(Ljava/lang/String;[I)V"), s, a);
        env->DeleteLocalRef(a);
        env->DeleteLocalRef(s);
        check();
    }

    void update_lookup_table(const LookupTable& table, bool visible) override {
        if (!ready()) return;
        jclass string_class = env->FindClass("java/lang/String");
        jsize n = static_cast<jsize>(table.get_number_of_candidates());
        jobjectArray a = env->NewObjectArray(n, string_class, nullptr);
        for (jsize i = 0; i < n; ++i) {
            jstring s = to_jstring(env, table.get_candidate(static_cast<size_t>(i)));
            env->SetObjectArrayElement(a, i, s);
            env->DeleteLocalRef(s);
        }
        env->CallVoidMethod(client, method("updateCandidates", "([Ljava/lang/String;IZ)V"), a,
                            static_cast<jint>(table.get_cursor_pos()), static_cast<jboolean>(visible));
        env->DeleteLocalRef(a);
        env->DeleteLocalRef(string_class);
        check();
    }

    void mode_changed(Mode mode) override {
        if (!ready()) return;
        env->CallVoidMethod(client, method("modeChanged", "(I)V"), static_cast<jint>(mode));
        check();
    }

private:
    ustring cache_;  // the text before the cursor as the engine has edited it

    bool ready() const { return env && client; }

    jmethodID method(const char* name, const char* sig) {
        jclass c = env->GetObjectClass(client);
        jmethodID m = env->GetMethodID(c, name, sig);
        env->DeleteLocalRef(c);
        return m;
    }

    // Reports and clears a Java exception thrown by a callback.
    bool check() {
        if (!env->ExceptionCheck()) return false;
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
};

struct Native {
    std::unique_ptr<Engine> engine;
    JavaClient client;
};

Native* native(jlong handle) { return reinterpret_cast<Native*>(handle); }

// Gives the engine the JNIEnv of the current call.
Engine& engine(JNIEnv* env, jlong handle) {
    Native* n = native(handle);
    n->client.env = env;
    return *n->engine;
}

}  // namespace

#define JNI(name) Java_works_firstfruits_hiragana_ime_NativeEngine_##name

extern "C" {

JNIEXPORT jlong JNICALL JNI(nativeCreate)(JNIEnv* env, jclass, jstring system_dir, jstring user_dir,
                                          jstring settings_path, jstring xkb) {
    log::set_sink([](log::Level level, const std::string& message) {
        int priority = level <= log::DEBUG     ? ANDROID_LOG_DEBUG
                       : level <= log::INFO    ? ANDROID_LOG_INFO
                       : level <= log::WARNING ? ANDROID_LOG_WARN
                                               : ANDROID_LOG_ERROR;
        __android_log_write(priority, TAG, message.c_str());
    });
    try {
        auto n = std::make_unique<Native>();
        Settings settings = Settings::load(fs::u8path(utf8_of(env, settings_path)));
        Engine::Paths paths{fs::u8path(utf8_of(env, system_dir)), fs::u8path(utf8_of(env, user_dir))};
        n->engine = std::make_unique<Engine>(paths, settings, utf8_of(env, xkb));
        n->engine->set_client(&n->client);
        return reinterpret_cast<jlong>(n.release());
    } catch (const std::exception& e) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "could not create the engine: %s", e.what());
        return 0;
    }
}

JNIEXPORT void JNICALL JNI(nativeDestroy)(JNIEnv* env, jclass, jlong handle) {
    Native* n = native(handle);
    n->client.env = env;
    n->engine.reset();  // saves the input history
    n->client.set(nullptr);
    delete n;
}

JNIEXPORT void JNICALL JNI(nativeSetClient)(JNIEnv* env, jclass, jlong handle, jobject client) {
    Native* n = native(handle);
    n->client.env = env;
    n->client.set(client);
}

JNIEXPORT jboolean JNICALL JNI(nativeProcessKey)(JNIEnv* env, jclass, jlong handle, jint keysym, jint keycode,
                                                 jint state) {
    return engine(env, handle).process_key_event(static_cast<keysym::Keysym>(keysym), static_cast<uint32_t>(keycode),
                                                 static_cast<uint32_t>(state));
}

JNIEXPORT jboolean JNICALL JNI(nativeInsertKana)(JNIEnv* env, jclass, jlong handle, jstring kana) {
    return engine(env, handle).insert_kana(from_jstring(env, kana));
}

JNIEXPORT jboolean JNICALL JNI(nativeCycleKana)(JNIEnv* env, jclass, jlong handle) {
    return engine(env, handle).insert_kana(Engine::KANA_CYCLE);
}

JNIEXPORT void JNICALL JNI(nativeCandidateClicked)(JNIEnv* env, jclass, jlong handle, jint index) {
    Engine& e = engine(env, handle);
    // Engine::candidate_clicked() takes an index in the current page; move to
    // the page that has the candidate first.
    const LookupTable& table = e.lookup_table();
    size_t i = static_cast<size_t>(index);
    if (table.get_number_of_candidates() <= i) return;
    size_t page = table.get_page_size();
    while (i / page < table.page_start() / page && e.page_up()) {
    }
    while (table.page_start() / page < i / page && e.page_down()) {
    }
    e.candidate_clicked(i % page);
}

JNIEXPORT void JNICALL JNI(nativeSetSurroundingSupported)(JNIEnv* env, jclass, jlong handle, jboolean supported) {
    engine(env, handle).set_surrounding_supported(supported);
}

JNIEXPORT void JNICALL JNI(nativeFocusIn)(JNIEnv* env, jclass, jlong handle) { engine(env, handle).focus_in(); }

JNIEXPORT void JNICALL JNI(nativeFocusOut)(JNIEnv* env, jclass, jlong handle) { engine(env, handle).focus_out(); }

JNIEXPORT void JNICALL JNI(nativeReset)(JNIEnv* env, jclass, jlong handle) { engine(env, handle).reset(); }

JNIEXPORT void JNICALL JNI(nativeCommitAll)(JNIEnv* env, jclass, jlong handle) { engine(env, handle).commit_all(); }

JNIEXPORT void JNICALL JNI(nativeCompositionTerminated)(JNIEnv* env, jclass, jlong handle) {
    engine(env, handle).composition_terminated();
}

JNIEXPORT jboolean JNICALL JNI(nativeHasPendingInput)(JNIEnv* env, jclass, jlong handle) {
    return engine(env, handle).has_pending_input();
}

JNIEXPORT jint JNICALL JNI(nativeGetMode)(JNIEnv* env, jclass, jlong handle) {
    return static_cast<jint>(engine(env, handle).get_mode());
}

JNIEXPORT void JNICALL JNI(nativeSetMode)(JNIEnv* env, jclass, jlong handle, jint mode, jboolean override) {
    engine(env, handle).set_mode(static_cast<Mode>(mode), override);
}

JNIEXPORT void JNICALL JNI(nativeApplySettings)(JNIEnv* env, jclass, jlong handle, jstring settings_path,
                                                jstring xkb) {
    Settings settings = Settings::load(fs::u8path(utf8_of(env, settings_path)));
    engine(env, handle).apply_settings(settings, utf8_of(env, xkb));
}

JNIEXPORT void JNICALL JNI(nativeReloadDictionary)(JNIEnv* env, jclass, jlong handle) {
    engine(env, handle).reload_dictionary();
}

}  // extern "C"
