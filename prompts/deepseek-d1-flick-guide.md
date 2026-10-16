# D1 フリックの方向ガイドと押したキーの拡大表示 — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。ひらがなで打ってから、あとで漢字に変換する日本語入力です。
キーボードは自前の `View`（Canvas で描画）で、QWERTY とフリック（12キー）の2種類があります。

いまのフリックは、指を離した位置で方向を決めるだけで、**指を動かしているあいだ何も表示されません**。
そのため、どちらに払えば何が出るのかが分かりません。

## 2. やってほしいこと

`app/src/main/java/works/firstfruits/hiragana/keyboard/KeyboardView.kt` に、次の2つを足してください。

1. **フリックの方向ガイド**
   - `KeyAction.Kana` のキーを押しているあいだ、そのキーを中心に十字形のガイドを描く。
   - ガイドは中央・左・上・右・下の5つのマスで、それぞれのかなを表示する（`KeyAction.Kana.kanaFor(direction)`）。null の方向は描かない。
   - 指を動かすたびに `FlickResolver.resolve(dx, dy, flickThreshold)` で方向を求め、いま選ばれているマスを色で強調する。
   - ガイドはキーボードの外にはみ出してもよい。ただし `View` の外には描けないので、はみ出す部分（上の行のさらに上など）は切れたままでかまわない。
2. **押したキーの拡大表示**
   - `KeyAction.Letter` のキーを押しているあいだ、キーの真上に、ラベルを大きくした吹き出しを描く。
   - いちばん上の行のキーは、上に描く場所がないので、キーの中に大きく描くだけでよい。

## 3. 触ってはいけないもの

- `KeyboardView.Listener` の形（`fun onKey(key: Key, direction: FlickDirection)`）と、`onKey` を呼ぶタイミング（指を離したとき。長押しの連続入力のときはくり返し）。
- `ime/` パッケージの全部（`HiraganaImeService.kt` などは別の担当です）。
- `app/src/main/cpp/` の全部。
- `KeyboardLayout.kt` の型（`Key`、`KeyAction`、`FlickDirection`）。

## 4. 手がかり

### いまの型（`keyboard/KeyboardLayout.kt` から抜粋）

```kotlin
sealed interface KeyAction {
    data class Letter(val char: Char) : KeyAction
    data class Kana(val flicks: List<String?>) : KeyAction {   // CENTER, LEFT, UP, RIGHT, DOWN の順
        fun kanaFor(direction: FlickDirection): String? = flicks.getOrNull(direction.ordinal)
    }
    // ほかに Convert, Okurigana, Katakana, Backspace などの data object
}
enum class FlickDirection { CENTER, LEFT, UP, RIGHT, DOWN }
data class Key(val label: String, val action: KeyAction, val width: Float = 1f, val hint: String = "", val repeatable: Boolean = false)
```

### `KeyboardView.kt` のいまのつくり

- `bounds: List<Pair<Key, RectF>>`：`onLayout` で計算した、各キーの四角。
- `pressed: Pair<Key, RectF>?`：押しているキー。`ACTION_DOWN` で決まり、`ACTION_UP` / `ACTION_CANCEL` で null になる。
- `downX`, `downY`：押し始めた座標。
- `flickThreshold = 24 * density`：これより短い動きはタップ。
- `ACTION_MOVE` はまだ処理していない。ここで現在の方向を覚え、`invalidate()` する。

### 描き方の目安

- ガイドの1マスは、キーと同じ大きさ。
- 色：ガイドの背景は濃いめの灰色（`#505860`）、文字は白、選ばれているマスは青（`#3366cc`）。
- 吹き出し：キーの幅の1.3倍、高さはキーの1.6倍、角丸。ラベルの文字はキーの2倍の大きさ。

## 5. 制約

- 新しいライブラリは足さない。Android SDK の `Canvas` / `Paint` だけで描く。
- `onDraw` の中で `Paint` や `RectF` を new しない（フィールドに持って使い回す）。
- Kotlin のコーディングスタイルは、いまのファイルに合わせる（4スペース、`private val`、英語のコメントを短く）。
- ロジック（どのマスをどこに描くか）は、できれば純粋な関数に分けて、`app/src/test/java/works/firstfruits/hiragana/keyboard/` に JUnit のテストを書く。例：`FlickGuide.cells(keyRect: RectF, ...)` のように。
  - ただし `RectF` は JVM の単体テストでは動かないので、テストする関数は `Float` の値で受け渡しする。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
