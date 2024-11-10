# MiningGame

Javaコースミニゲーム開発編　「鉱石採掘ゲーム」

## デモ動画
<video src="[https://github.com/user-attachments/assets/88007f69-c0a1-44ed-bf8a-41ad14110c11]" controls="controls" style="max-width: 100%;">
  Your browser does not support the video tag.
</video>

## ゲームの概要
- 制限時間内（60秒）まで鉱石を採掘し、点数を競うミニゲーム
- /mining gameとコマンドを入力するとゲームが開始します
- ゲームが開始されると、プレイヤーの５ブロック先に５×５×５の範囲でランダムに鉱石が出現します
- 鉱石の種類により点数がと出現率が決まっています
- 石（1点）、鉄（5点）、銅（10点）、ラピスラズリ（15点）、レッドストーン（20点）、エメラルド（30点）、ゴールド（40点）、ダイヤモンド（50点）、その他（０点）
- /mining game listと入力するとスコアが表示されます
- プレイヤーのインベントリ情報をゲーム開始前に保存し、終了後に復元します

# MiningGame データベース設計

このプロジェクトでは、MySQLを使用してプレイヤーのスコアを管理します。以下はデータベースの設計と設定方法です。

## データベース設計

| 属性      | 設定値             |
|---------|-----------------|
| ユーザー名   | ※               |
| パスワード   | ※               |
| メールアドレス | ※               |
| データベース名 | `spigot_server` |
| テーブル名   | `player_score`  |

## データベースの接続方法

MySQLに接続し、以下のコマンドを実行してデータベースとテーブルを作成します。

   ```sql
   CREATE DATABASE spigot_server;
   USE spigot_server;
   CREATE TABLE player_score (
       id INT AUTO_INCREMENT,
       score INT,
       registered_at DATETIME,
       player_name VARCHAR(255),
       PRIMARY KEY (id)
   ) DEFAULT CHARSET=utf8;
