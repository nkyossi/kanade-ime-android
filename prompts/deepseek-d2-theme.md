# D2 ダークテーマとキーボードの高さ — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。**D1（フリックの方向ガイド）が終わってから**渡してください。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。キーボード（`keyboard/KeyboardView.kt`）と候補の列（`keyboard/CandidateBar.kt`）は、色をコードに直接書いています。そのため、

- 端末をダークモードにしても、キーボードだけ白いまま
- キーの高さが 52dp で固定なので、手の小さい子どもや、大きい画面のタブレットに合わない

という問題があります。

## 2. やってほしいこと

1. **色をテーマにまとめる**
   - 新しいファイル `keyboard/KeyboardTheme.kt` に、色をまとめた `data class KeyboardTheme` を作る。
   - 持つ色：キーボードの背景、キー、押したキー、特別なキー（変換・確定・⌫ などの、文字でないキー）、キーの文字、ヒントの文字、候補の列の背景、候補の文字、選ばれた候補の背景、モード表示の文字、フリックガイドの背景・文字・選ばれたマス（D1 で足したもの）。
   - `KeyboardTheme.light` と `KeyboardTheme.dark` の2つを用意する。light はいまの色と同じにする。
   - `fun KeyboardTheme.Companion.forContext(context: Context): KeyboardTheme`：`Configuration.UI_MODE_NIGHT_MASK` を見て light / dark を返す。
2. **`KeyboardView` と `CandidateBar` がテーマを使うようにする**
   - どちらにも `var theme: KeyboardTheme` を足し、代入されたら描き直す。
   - 初期値は `KeyboardTheme.forContext(context)`。
   - 特別なキーの判定は `key.action !is KeyAction.Letter && key.action !is KeyAction.Kana && key.action !is KeyAction.Text` とする。
3. **キーの高さを変えられるようにする**
   - `KeyboardView` に `var rowHeightDp: Float`（既定 52）を足し、代入されたら `requestLayout()` する。
   - 範囲は 40〜72 にする（範囲外は丸める）。
   - 範囲を丸める関数は純粋な関数にして、JUnit でテストする。
4. テスト：`app/src/test/java/works/firstfruits/hiragana/keyboard/KeyboardThemeTest.kt`
   - light と dark で、キーの色と文字の色のコントラスト比が 4.5 以上であること（コントラスト比の計算は、テストの中で WCAG の式を書く。`android.graphics.Color` は JVM の単体テストでは動かないので、色は `Int`（0xAARRGGBB）のまま扱う）。
   - 高さの丸め。

## 3. 触ってはいけないもの

- `ime/` パッケージの全部（`HiraganaImeService.kt` が `KeyboardView` と `CandidateBar` を作っています。テーマや高さを渡す処理は、あとで別の人が足します）。
- `app/src/main/cpp/` の全部。
- `KeyboardView.Listener` と `CandidateBar.Listener` の形、`KeyboardLayout.kt` の型。

## 4. 手がかり

- いまの色（light）
  - 背景 `#d8dbe0`、キー `#fafafa`、押したキー `#c8d8f0`、文字 `#202020`、ヒント `#808080`
  - 候補の列の背景 `#f0f1f4`、選ばれた候補 `#c8d8f0`、モード表示 `#3366cc`
- dark の目安：背景 `#1e2024`、キー `#3a3d42`、特別なキー `#2b2e33`、押したキー `#4a6a9a`、文字 `#f0f0f0`、ヒント `#a0a4aa`。
- `Paint` の色は、テーマが変わったときに `Paint.color` を入れ替えるだけにする（`onDraw` で new しない）。

## 5. 制約

- 新しいライブラリは足さない。
- XML のテーマや `styles.xml` ではなく、Kotlin のコードで色を持つ（IME のビューはコードで組み立てているため）。
- コードのスタイルはいまのファイルに合わせる（4スペース、英語のコメントを短く）。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
