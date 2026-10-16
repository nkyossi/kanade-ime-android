# D3 記号・数字のキーボード — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。ひらがなで打ってから、あとで漢字に変換する日本語入力です。
キーボードは QWERTY（ローマ字）とフリック（12キー）の2つがありますが、**数字と記号を打つキーボードがありません**。

キーボードはデータ（`KeyboardLayout`）で定義し、`KeyboardView` がそれを描きます。
`KeyAction.SwitchLayout(id)` のキーを押すと、`Layouts.byId(id)` のキーボードに切り替わります。

## 2. やってほしいこと

1. 新しいファイル `app/src/main/java/works/firstfruits/hiragana/keyboard/SymbolLayout.kt` に、2つのキーボードを作る。
   - **`SymbolLayout.numbers`**（id `"numbers"`）
     - 数字 `1234567890` の行
     - よく使う記号の行：`- / : ; ( ) ¥ & @ "`
     - `. , ? ! '` の行（左はしに「記号」へ切り替えるキー、右はしに ⌫）
     - いちばん下の行：「ABC」（QWERTY へ）、「あいう」（フリックへ）、「空白」（`KeyAction.Space`）、「確定」（`KeyAction.Enter`）
   - **`SymbolLayout.symbols`**（id `"symbols"`）
     - `[ ] { } # % ^ * + =`
     - `_ \ | ~ < > € £ • …`
     - 全角の記号：`「 」 『 』 【 】 ・ ～ ※ 〒`
     - いちばん下の行は `numbers` と同じ（ただし「123」で `numbers` に戻る）。
   - 記号のキーは `KeyAction.Text(text)` にする。`KeyAction.Text` は、エンジンを通さずにそのまま確定する文字です。
   - 数字と、`- / : ; ( ) & @ " . , ? ! '` は `KeyAction.Letter(char)` にする（ローマ字かなモードのときに、全角や「ー」「、」「。」になる、通常の文字キーと同じ扱いにするため）。
2. `keyboard/Layouts.kt` の `all` に、この2つを足す。
3. `keyboard/QwertyLayout.kt` と `keyboard/FlickLayout.kt` に、`numbers` へ切り替えるキー（ラベル「123」）を1つずつ足す。
   - QWERTY：いちばん下の行の「、」キーを「123」に置き換える（「、」は `numbers` の `,` で打てる）。
   - フリック：左下の「ABC」キーの右にある「゛゜小」キーは残し、いちばん下の行のいちばん左に「123」を足す（その行のキーが6つになる。幅はほかのキーと同じでよい）。
4. JUnit のテスト `app/src/test/java/works/firstfruits/hiragana/keyboard/LayoutsTest.kt` を書く。
   - すべてのキーボードについて、`SwitchLayout` の行き先が `Layouts` に登録されている id であること。
   - どのキーボードにも ⌫ と確定（`KeyAction.Enter`）があること。
   - 各行のキーの数が 0 でないこと。

## 3. 触ってはいけないもの

- `KeyboardLayout.kt` の型（`Key`、`KeyAction`、`KeyRow`、`KeyboardLayout`）。足りないものがあれば、変えずに報告してください。
- `ime/` パッケージの全部（`HiraganaImeService.kt` などは別の担当です）。
- `app/src/main/cpp/` の全部。
- `KeyboardView.kt`（ほかの人が同時に触っています）。

## 4. 手がかり

### いまの型（`keyboard/KeyboardLayout.kt` から抜粋）

```kotlin
sealed interface KeyAction {
    data class Letter(val char: Char) : KeyAction
    data class Kana(val flicks: List<String?>) : KeyAction
    data class Text(val text: String) : KeyAction
    data class SwitchLayout(val id: String) : KeyAction
    data object Convert : KeyAction
    data object Okurigana : KeyAction
    data object Katakana : KeyAction
    data object Shrink : KeyAction
    data object Expand : KeyAction
    data object Enter : KeyAction
    data object Cancel : KeyAction
    data object Backspace : KeyAction
    data object Space : KeyAction
    data object Shift : KeyAction
    data object KanaCycle : KeyAction
    data object ToggleMode : KeyAction
    data object CursorLeft : KeyAction
    data object CursorRight : KeyAction
}
data class Key(val label: String, val action: KeyAction, val width: Float = 1f, val hint: String = "", val repeatable: Boolean = false)
data class KeyRow(val keys: List<Key>)
data class KeyboardLayout(val id: String, val rows: List<KeyRow>) {
    companion object { const val QWERTY = "qwerty"; const val FLICK = "flick" }
}
```

### 書き方のお手本

`keyboard/QwertyLayout.kt` の書き方に合わせてください（`object` の中に `val layout = KeyboardLayout(...)`、文字列から `Key` を作る小さな関数）。
⌫ は `Key("⌫", KeyAction.Backspace, 1.5f, repeatable = true)` です。

`KeyboardLayout` の `companion object` に id を足したくなったら、足さずに `SymbolLayout` の中に `const val NUMBERS = "numbers"` のように置いてください。

## 5. 制約

- 新しいライブラリは足さない。
- コードのスタイルはいまのファイルに合わせる（4スペース、英語のコメントを短く）。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
