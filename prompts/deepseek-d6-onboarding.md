# D6 起動画面（キーボードを使えるようにする案内） — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。Android のキーボード（IME）は、入れただけでは使えず、

1. 端末の設定で「有効」にする
2. 入力欄で「切り替える」

の2つが必要です。いまの起動画面（`MainActivity.kt`）には、ボタンが2つと入力欄が1つあるだけで、**いまどこまでできているのかが分かりません**。

使う人は子どもや、その保護者・先生を想定しています。

## 2. やってほしいこと

`app/src/main/java/works/firstfruits/hiragana/MainActivity.kt` を、3つの手順が順に並ぶ画面にしてください。

1. **手順1「キーボードを有効にする」**
   - このIMEが有効かどうかを `InputMethodManager.getEnabledInputMethodList()` で調べる（自分のパッケージ名の IME があるか）。
   - 有効でなければ「設定をひらく」ボタン（`Settings.ACTION_INPUT_METHOD_SETTINGS`）。有効なら「✓ できました」と表示する。
2. **手順2「キーボードを切り替える」**
   - いま選ばれている IME を `Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)` で調べる（自分のサービス `works.firstfruits.hiragana/.ime.HiraganaImeService` を含むか）。
   - 選ばれていなければ「キーボードをえらぶ」ボタン（`InputMethodManager.showInputMethodPicker()`）。選ばれていれば「✓ できました」。
   - 手順1が終わっていないあいだは、このボタンを押せないようにする。
3. **手順3「ためしてみる」**
   - 入力欄（いまある `EditText`）と、その下に短い説明：「ひらがなで うってから［変換］を おすと、漢字に なります。」
4. **状態の更新**
   - 設定から戻ってきたとき（`onResume`）と、IME を選んだあと（`onWindowFocusChanged(true)`）に、手順1・2の表示を更新する。
5. 状態を判定する部分は、純粋な関数に分けて JUnit でテストする。
   - 例：`fun isImeId(id: String?, packageName: String): Boolean`
   - `DEFAULT_INPUT_METHOD` の値の形は `works.firstfruits.hiragana/.ime.HiraganaImeService` または `works.firstfruits.hiragana/works.firstfruits.hiragana.ime.HiraganaImeService` のどちらもあり得る。両方に対応させる。
   - テストは `app/src/test/java/works/firstfruits/hiragana/OnboardingTest.kt`

## 3. 触ってはいけないもの

- `ime/`、`keyboard/`、`settings/` パッケージ、`app/src/main/cpp/` の全部。
- `AndroidManifest.xml` の `<service>` の部分。

## 4. 手がかり

- いまの `MainActivity` は `android.app.Activity` を継承し、レイアウトをコードで組み立てています。XML レイアウト（`res/layout/activity_main.xml`）に変えてもかまいません。
- 文字列は `res/values/strings.xml` に置いてください。いまある `open_ime_settings`、`choose_ime`、`try_here` は、使わなくなったら消してかまいません。
- 文字は、小学校低学年でも読めるよう、ひらがなを多めにしてください（「設定」「変換」「漢字」は漢字のままでよい）。
- 1行が長くなりすぎないように、文字の大きさは 18sp 前後にしてください。

## 5. 制約

- 新しいライブラリは足さない（`androidx.core` はすでに使えます）。
- コードのスタイルはいまのファイルに合わせる（4スペース、英語のコメントを短く）。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
