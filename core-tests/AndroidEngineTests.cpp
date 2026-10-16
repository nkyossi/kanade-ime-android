// Engine tests for the parts that are new in the Android port.

#include <cstring>
#include <filesystem>
#include <random>
#include <string>
#include <vector>

#include "Engine.h"
#include "Keysyms.h"
#include "Test.h"

using namespace hiragana;
using namespace hiragana::keysym;
namespace fs = std::filesystem;

namespace {

class Document : public Client {
public:
    ustring document;
    index_t cursor = 0;
    ustring preedit;
    std::vector<ustring> candidates;

    void get_surrounding_text(ustring& text, index_t& pos) override {
        text = document;
        pos = cursor;
    }
    void delete_surrounding_text(index_t size) override {
        index_t start = cursor - size;
        document = slice_to(document, start) + slice_from(document, cursor);
        cursor = start;
    }
    void commit_text(const ustring& text) override {
        document = slice_to(document, cursor) + text + slice_from(document, cursor);
        cursor += len(text);
    }
    void update_preedit(const Preedit& p) override { preedit = p.text(); }
    void update_lookup_table(const LookupTable& table, bool) override {
        candidates.clear();
        for (size_t i = 0; i < table.get_number_of_candidates(); ++i) candidates.push_back(table.get_candidate(i));
    }
};

uint32_t keycode_of(char c) {
    const char* rows[] = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    const uint32_t starts[] = {16, 30, 44};
    for (int r = 0; r < 3; ++r) {
        const char* p = std::strchr(rows[r], c);
        if (p) return starts[r] + static_cast<uint32_t>(p - rows[r]);
    }
    return 0;
}

struct Fixture {
    fs::path user_dir;
    Document client;
    std::unique_ptr<Engine> engine;

    explicit Fixture(bool surrounding = true, bool shorter = false) {
        user_dir = fs::temp_directory_path() / ("hiragana-android-test-" + std::to_string(std::random_device{}()));
        fs::create_directories(user_dir);
        Settings s;
        s.mode = "あ";
        s.user_dictionary = "";
        engine = std::make_unique<Engine>(Engine::Paths{fs::u8path(UPSTREAM_DIR), user_dir}, s, "us");
        engine->set_client(&client);
        engine->set_surrounding_supported(surrounding);
        engine->set_shorter_candidates(shorter);
    }
    ~Fixture() {
        engine.reset();
        std::error_code ec;
        fs::remove_all(user_dir, ec);
    }

    void key(Keysym keyval, uint32_t keycode, uint32_t state = 0) {
        engine->process_key_event(keyval, keycode, state);
        engine->process_key_event(keyval, keycode, state | RELEASE_MASK);
    }
    void type(const char* s) {
        for (; *s; ++s) key(static_cast<Keysym>(*s), keycode_of(*s));
    }
    void kana(const ustring& s) {
        for (char32_t c : s) engine->insert_kana(ustring(1, c));
    }
    void cycle() { engine->insert_kana(Engine::KANA_CYCLE); }
    ustring state() const { return client.document + U"|" + client.preedit; }
};

}  // namespace

TEST(android_insert_kana_converts_like_roomazi) {
    Fixture a, b;
    a.type("kanji");
    b.kana(U"かんじ");
    EXPECT_EQ(b.client.document, ustring(U"かんじ"));
    a.key(space, 57);
    b.key(space, 57);
    EXPECT_EQ(b.client.preedit, ustring(U"漢字"));
    EXPECT_EQ(a.state(), b.state());
    EXPECT_TRUE(a.client.candidates == b.client.candidates);
    a.key(Return, 28);
    b.key(Return, 28);
    EXPECT_EQ(b.client.document, ustring(U"漢字"));
}

TEST(android_insert_kana_okurigana_like_roomazi) {
    Fixture a, b;
    a.type("ka");
    b.kana(U"か");
    a.key(Henkan, 92, SHIFT_MASK);
    b.key(Henkan, 92, SHIFT_MASK);
    EXPECT_EQ(a.state(), b.state());
    a.type("ku");
    b.kana(U"く");
    EXPECT_EQ(a.state(), b.state());
    EXPECT_TRUE(a.client.candidates == b.client.candidates);
    // The complete okurigana confirms the only candidate.
    EXPECT_EQ(b.state(), ustring(U"書く|"));
}

TEST(android_insert_kana_commits_candidate) {
    // Typing kana while a candidate is shown confirms the candidate first.
    Fixture a, b;
    a.type("kanji");
    b.kana(U"かんじ");
    a.key(space, 57);
    b.key(space, 57);
    a.type("wo");
    b.kana(U"を");
    EXPECT_EQ(b.client.document, ustring(U"漢字を"));
    EXPECT_EQ(a.state(), b.state());
}

TEST(android_kana_cycle) {
    Fixture f;
    f.kana(U"か");
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"が"));
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"か"));

    f.kana(U"つ");
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かっ"));
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かづ"));
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かつ"));

    f.kana(U"は");
    f.cycle();
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かつぱ"));
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かつは"));

    // Nothing to cycle
    f.kana(U"ん");
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"かつはん"));
}

TEST(android_kana_cycle_katakana) {
    Fixture f;
    f.engine->set_mode(MODE_KATAKANA, false);
    f.kana(U"は");
    EXPECT_EQ(f.client.document, ustring(U"ハ"));
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U"バ"));
}

TEST(android_kana_cycle_at_start_does_nothing) {
    Fixture f;
    f.cycle();
    EXPECT_EQ(f.client.document, ustring(U""));
}

TEST(android_insert_kana_legacy_preedit) {
    Fixture f(false);
    f.kana(U"かんじ");
    EXPECT_EQ(f.client.preedit, ustring(U"かんじ"));
    f.key(space, 57);
    EXPECT_EQ(f.client.preedit, ustring(U"漢字"));
}

// Examples used in docs/usage.md
TEST(android_docs_examples) {
    {
        Fixture f;
        f.type("terebi");
        f.key(Shift_R, 54);
        EXPECT_EQ(f.state(), ustring(U"てれ|ビ"));
        f.key(Shift_R, 54);
        EXPECT_EQ(f.state(), ustring(U"て|レビ"));
        f.key(Shift_R, 54);
        EXPECT_EQ(f.state(), ustring(U"|テレビ"));
        f.key(Return, 28);
        EXPECT_EQ(f.state(), ustring(U"テレビ|"));
    }
    {
        Fixture f;
        f.type("kyou");
        f.key(space, 57);
        EXPECT_EQ(f.state(), ustring(U"|今日"));
    }
    {
        Fixture f;
        f.type("kyouha");
        f.key(space, 57);
        f.key(Tab, 15);
        EXPECT_EQ(f.state(), ustring(U"きょう|羽"));
        f.key(Tab, 15);
        EXPECT_EQ(f.state(), ustring(U"きょう|羽"));
    }
    {
        Fixture f;
        f.type("kanji");
        f.key(space, 57);
        EXPECT_TRUE(f.client.candidates.size() > 1);
        f.key(space, 57);
        EXPECT_EQ(f.state(), ustring(U"|幹事"));
    }
}

// Shorter readings in the candidate list
namespace {
index_t index_of(const std::vector<ustring>& v, const ustring& s) {
    for (size_t i = 0; i < v.size(); ++i)
        if (v[i] == s) return static_cast<index_t>(i);
    return -1;
}
}  // namespace

TEST(android_shorter_candidates_are_listed) {
    Fixture f(true, true);
    f.type("kyouha");
    f.key(space, 57);
    EXPECT_EQ(f.state(), ustring(U"きょ|右派"));
    // 右派 is the only candidate of うは; the ones of は follow it.
    EXPECT_EQ(f.client.candidates.front(), ustring(U"右派"));
    EXPECT_TRUE(0 < index_of(f.client.candidates, U"う羽"));
}

TEST(android_shorter_candidate_selected) {
    Fixture f(true, true);
    f.type("kyouha");
    f.key(space, 57);
    index_t i = index_of(f.client.candidates, U"う羽");
    EXPECT_TRUE(0 < i);
    f.engine->candidate_selected(static_cast<size_t>(i));
    EXPECT_EQ(f.state(), ustring(U"きょう羽|"));
    EXPECT_TRUE(f.client.candidates.empty());
    EXPECT_TRUE(!f.engine->has_pending_input());
}

TEST(android_shorter_candidates_by_henkan) {
    // Pressing Henkan past the last candidate of the reading shrinks it.
    Fixture f(true, true);
    f.type("kyouha");
    f.key(space, 57);
    f.key(space, 57);
    EXPECT_EQ(f.client.document, ustring(U"きょう"));
    EXPECT_TRUE(!f.client.preedit.empty());
    EXPECT_TRUE(0 <= index_of(f.client.candidates, f.client.preedit));
    f.key(Return, 28);
    EXPECT_EQ(f.client.preedit, ustring(U""));
    EXPECT_EQ(slice_to(f.client.document, 3), ustring(U"きょう"));
    EXPECT_TRUE(3 < len(f.client.document));
}

TEST(android_shorter_candidates_main_selection_unchanged) {
    Fixture f(true, true);
    f.type("kanji");
    f.key(space, 57);
    index_t i = index_of(f.client.candidates, U"幹事");
    EXPECT_TRUE(0 < i);
    f.engine->candidate_selected(static_cast<size_t>(i));
    EXPECT_EQ(f.state(), ustring(U"幹事|"));
}

TEST(android_shorter_candidates_off_by_default) {
    Fixture f;
    f.type("kyouha");
    f.key(space, 57);
    EXPECT_TRUE(f.client.candidates.empty());
}
