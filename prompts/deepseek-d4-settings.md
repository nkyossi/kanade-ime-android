# D4 設定画面 — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。ひらがなで打ってから、あとで漢字に変換する日本語入力です。
変換エンジンは C++ で、設定を `filesDir/user/settings.json` から読みます。**設定画面がまだないので、作ってください。**

設定の受け渡しはファイルだけで行います。

- 設定画面は `settings.json` を書くだけ。
- IME（`HiraganaImeService`）は、テキスト欄にフォーカスが来るたびに `settings.json` の更新時刻を見て、変わっていれば読み直します（この部分は作ってあるので、触らないでください）。

## 2. やってほしいこと

新しいパッケージ `app/src/main/java/works/firstfruits/hiragana/settings/` に、次のものを作ってください。

1. **`ImeSettings.kt`**：設定の値を持つ `data class` と、JSON との読み書き。
   - `fun load(file: File): ImeSettings`：ファイルがない、または壊れているときは既定値を返す。
   - `fun save(file: File)`：
     - **ファイルにある、知らないキーは消さずに残す。** 特に `history-generation` は、学習のリセットに使う数なので、必ずそのまま残す。
     - 書き込みは、同じフォルダの一時ファイルに書いてから `renameTo` する（途中で落ちても壊れないように）。
   - JSON は `org.json.JSONObject` を使う。JVM の単体テストで `org.json` を本物として使えるように、`app/build.gradle.kts` に `testImplementation("org.json:json:20240303")` を足す（`gradle/libs.versions.toml` に登録する形で）。
2. **`SettingsActivity.kt`**：`androidx.preference` の `PreferenceFragmentCompat` を使った設定画面。
   - 値は SharedPreferences ではなく `ImeSettings` で読み書きする。`PreferenceDataStore` を実装したクラスを作り、`preferenceManager.preferenceDataStore` に設定すること。
   - 画面に並べる項目（上から）：

     | 見出し | 種類 | JSON のキー | 選択肢・既定値 |
     |---|---|---|---|
     | はじめのモード | ListPreference | `mode` | 「ひらがな」=`あ`（既定）、「英字」=`A` |
     | 漢字の範囲 | ListPreference | `dictionary` | 「小学1年」=`restrained.1.dic` … 「小学6年」=`restrained.6.dic`、「中学」=`restrained.7.dic`、「高校」=`restrained.8.dic`、「一般（常用漢字）」=`restrained.9.dic`（既定） |
     | 許容の送り仮名も使う | SwitchPreference | `permissible` | 既定 false。説明文「一般（常用漢字）のときだけ使えます」 |
     | 数字を半角にする | SwitchPreference | `use-half-width-digits` | 既定 false |
     | 「nn」で「ん」にする | SwitchPreference | `nn-as-jis-x-4063` | 既定 false |
     | ユーザー辞書 | Preference（押すと画面を開く） | — | いまは押しても何もしない。key は `"user_dictionary"` にしておく（あとで D5 がつなぎます） |

   - 画面の XML は `app/src/main/res/xml/preferences.xml`、文字列は `res/values/strings.xml` に足す。
3. **IME の設定ボタンとつなぐ**
   - `app/src/main/res/xml/method.xml` の `<input-method>` に `android:settingsActivity="works.firstfruits.hiragana.settings.SettingsActivity"` を足す。
   - `AndroidManifest.xml` に `SettingsActivity` を登録する（`android:exported="true"`、ラベルは「ひらがなIMEの設定」）。
   - テーマは、AppCompat を使うなら `Theme.AppCompat.DayNight` 系にする（`androidx.appcompat` を依存に足す）。
4. **テスト** `app/src/test/java/works/firstfruits/hiragana/settings/ImeSettingsTest.kt`
   - 既定値（ファイルがないとき）
   - 保存して読み直すと同じ値になること
   - `history-generation` と、知らないキー（例：`"foo": 1`）が保存しても残ること
   - 壊れた JSON のとき既定値になること

## 3. 触ってはいけないもの

- `ime/` パッケージの全部。特に `ime/SettingsJson.kt` と `ime/AssetInstaller.kt` は IME 側のコードなので、変えないでください。
  - `AssetInstaller.settingsFile(context)` が `settings.json` の場所を返します。これは**使ってよい**です。
- `app/src/main/cpp/` の全部（設定を読む C++ 側。キーの名前は下の表のとおりです）。
- `keyboard/` パッケージの全部。

## 4. 手がかり

### settings.json の形（C++ の `Settings::save` が書く形）

```json
{
  "mode": "あ",
  "dictionary": "restrained.9.dic",
  "user-dictionary": "my.dic",
  "layout": "roomazi",
  "altgr": "altgr",
  "keyboard": "us",
  "permissible": false,
  "use-half-width-digits": false,
  "nn-as-jis-x-4063": false,
  "combining-circumflex": false,
  "combining-macron": false,
  "use-llm": false,
  "use-cuda": false,
  "logging-level": "WARNING",
  "history-generation": 0
}
```

- 今回の画面で扱うのは `mode`、`dictionary`、`permissible`、`use-half-width-digits`、`nn-as-jis-x-4063` だけです。ほかのキーは読んだ値をそのまま書き戻してください。
- `layout` は `"roomazi"`、`keyboard` は `"us"` のまま変えないでください（ソフトキーボードは US 配列のキーを送っているため）。

### すでにある依存

`gradle/libs.versions.toml` に `androidx-preference`（`preference-ktx` 1.2.1）があり、`app/build.gradle.kts` で `implementation` されています。

### 環境

- AGP 9.0.1、Gradle 9.1.0、Kotlin は AGP に組み込みのもの（`kotlin-android` プラグインは使っていません。足さないでください）。
- minSdk 26、compileSdk 36。

## 5. 制約

- 新しく足してよい依存は `androidx.appcompat`（`implementation`）と `org.json:json`（`testImplementation`）だけ。
- コードのスタイルはいまのファイルに合わせる（4スペース、英語のコメントを短く）。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
