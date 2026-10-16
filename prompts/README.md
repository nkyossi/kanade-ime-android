# 他のAIに任せる作業のプロンプト

作成日：2026-10-16

土台（C++ エンジンの NDK ビルド、JNI、IME サービス、最小のキーボード）は Claude が作りました。
ここにあるプロンプトは、残りの作業のうち、仕様がはっきりしているものです。

## 使い方

1. 各ファイルの `---` より下を、そのまま相手のAIに貼ってください。
2. 1回に1本だけ渡してください。終わったら `git diff` を Claude に見せて、レビューを受けてから commit します。
3. 「順番」の列が同じ番号のものは、同時に進めてもぶつかりません。

## DeepSeek v4.1 Flash（コード）

| 順番 | ファイル | 内容 | 触るファイル |
|---|---|---|---|
| 1 | `deepseek-d1-flick-guide.md` | フリックの方向ガイドと、押したキーの拡大表示 | `KeyboardView.kt` ほか |
| 1 | `deepseek-d3-symbol-layouts.md` | 記号・数字のキーボード | `SymbolLayout.kt` ほか（新規） |
| 1 | `deepseek-d4-settings.md` | 設定画面 | `settings/` パッケージ（新規） |
| 1 | `deepseek-d6-onboarding.md` | 起動画面（有効化の案内） | `MainActivity.kt` |
| 2 | `deepseek-d2-theme.md` | ダークテーマ、キーボードの高さ | `KeyboardView.kt`、`CandidateBar.kt` |
| 2 | `deepseek-d5-user-dictionary.md` | ユーザー辞書の編集と、学習のリセット | `settings/` パッケージ |

- d2 は d1 のあとに（同じ `KeyboardView.kt` を触るため）。
- d5 は d4 のあとに（d4 の設定画面から開くため）。

## Sarashina AI（日本語の文章）

| ファイル | できるもの |
|---|---|
| `sarashina-s1-readme.md` | `README.md` |
| `sarashina-s2-usage.md` | `docs/usage.md`（使い方） |
| `sarashina-s3-privacy-store.md` | `docs/privacy.md` と `docs/store-listing.md` |

Sarashina はコードを読まない前提です。どのプロンプトも、最後に `sarashina-facts.md`（事実シート）の中身を貼って使います。
機能を変えたら、先に事実シートを直してください。

## AIに任せないもの（Claude がやる）

- `app/src/main/cpp/` の C++ と JNI
- `ime/HiraganaImeService.kt`、`ime/InputConnectionClient.kt`、`ime/NativeEngine.kt`
- 物理キーボードへの対応（キーイベントの変換、浮動候補ウィンドウ）
