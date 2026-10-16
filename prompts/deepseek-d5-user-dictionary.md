# D5 ユーザー辞書の編集と、学習のリセット — DeepSeek 用

> 使い方：以下の「---」から下をそのまま貼る。**D4（設定画面）が終わってから**渡してください。
> 作成日：2026-10-16

---

作業対象：`C:\Users\yossi\OneDrive\Documents\app\homemade\hiragana-ime-android`（Android アプリ、Kotlin + C++/NDK、git 管理）

## 1. 背景

「ひらがなIME」の Android 版です。ひらがなで打ってから、あとで漢字に変換する日本語入力です。
変換エンジン（C++）は、次の2つのファイルを `filesDir/user/` に持っています。

- **ユーザー辞書** `filesDir/user/my.dic`：使う人が自分で単語を足すファイル（例：名前、学校名）。
- **学習の記録** `filesDir/user/dic/*.dic`：選んだ候補の順番をエンジンが覚えておくファイル。

どちらも編集する画面がないので、作ってください。

IME（`HiraganaImeService`）は、テキスト欄にフォーカスが来るたびに、`my.dic` と `settings.json` の更新時刻を見て、変わっていれば読み直します（この部分は作ってあるので、触らないでください）。

## 2. やってほしいこと

1. **`settings/UserDictionary.kt`**：`my.dic` の読み書き（純粋な Kotlin、Android に依存しない）。
   - 形式は SKK 辞書と同じ。1行が1つの読みで、`よみ /候補1/候補2/` の形。
   - `;` で始まる行はコメント。読み書きのときに消さずに残す。
   - 読みはひらがなだけにする。候補に `/` と `;` は使えない（入力のときにはじく）。
   - 同じ読みが2回出てきたら、1行にまとめる。
   - 改行は `\n`、文字コードは UTF-8（BOMなし）。
   - 書き込みは、同じフォルダの一時ファイルに書いてから `renameTo` する。
2. **`settings/UserDictionaryActivity.kt`**：ユーザー辞書の画面。
   - 登録してある語の一覧（「よみ → 候補」の形で1行ずつ）。
   - 「追加」ボタンで、よみと候補を入れるダイアログを出す。
   - 一覧の行を長押しすると、消すか聞くダイアログを出す。
   - 変えたらすぐ保存する。
3. **学習のリセット**：`settings/HistoryReset.kt` に `fun clearHistory(userDir: File, settingsFile: File)` を作る。手順は Windows 版と同じにする。
   1. `settings.json` の `history-generation` を 1 増やして保存する（**先に**。動いている IME が古い学習を書き戻さないようにするため）。D4 で作った `ImeSettings` に `historyGeneration` がなければ、足してください。
   2. `userDir/dic/` の中の `.dic` ファイルの中身を、`; v1.0.0\n` の1行だけにする（ファイルは消さない）。
   3. `userDir/dic/` の中の `.bak` と `.tmp` のファイルを消す。
4. **設定画面とつなぐ**
   - D4 の設定画面の「ユーザー辞書」（key `"user_dictionary"`）を押したら `UserDictionaryActivity` を開く。
   - 設定画面に「学習をリセット」を足す。押したら「これまでに おぼえた 変換の じゅんばんを けします。よろしいですか？」と確認してから `clearHistory` を呼ぶ。
   - `UserDictionaryActivity` を `AndroidManifest.xml` に登録する（`android:exported="false"`）。
5. **テスト**（`app/src/test/java/works/firstfruits/hiragana/settings/`）
   - `UserDictionaryTest.kt`：読み込み、コメントが残ること、同じ読みがまとまること、追加・削除、保存して読み直すと同じになること、よみがひらがなでないとはじくこと。
   - `HistoryResetTest.kt`：一時フォルダに `settings.json` と `dic/restrained.9.dic`、`dic/restrained.9.dic.bak` を作って `clearHistory` を呼び、`history-generation` が増えること、`.dic` が `; v1.0.0\n` になること、`.bak` が消えることを確かめる。

## 3. 触ってはいけないもの

- `ime/` パッケージの全部。`AssetInstaller.userDir(context)` と `AssetInstaller.settingsFile(context)` は**使ってよい**です。
- `app/src/main/cpp/` の全部。
- `keyboard/` パッケージの全部。
- `filesDir/system/`（アプリに入っている辞書。読み取り専用として扱う）。

## 4. 手がかり

### my.dic の例

```
; ユーザー辞書
やまだ /山田/
さくらしょうがっこう /さくら小学校/桜小学校/
```

### 学習の記録ファイル（`filesDir/user/dic/restrained.9.dic` など）

- エンジンが書くファイルです。中身の形式は気にしなくてよく、リセットのときに `; v1.0.0\n` だけにすればよいです（C++ の `Dictionary.cpp` の `kDictionaryVersion` が `"v1.0.0"` です）。

## 5. 制約

- 新しいライブラリは足さない（D4 で足した `androidx.appcompat` と `androidx.preference` は使えます）。
- 文字は、小学校低学年でも読めるよう、ひらがなを多めにしてください（「辞書」「変換」「学習」は漢字のままでよい）。
- コードのスタイルはいまのファイルに合わせる（4スペース、英語のコメントを短く）。

## 6. 完了条件

PowerShell で次の2つが通ること。

```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

最後に、変えたファイルの一覧と、どう確かめたかを短く書いてください。commit はしないでください（レビューのあとで行います）。
