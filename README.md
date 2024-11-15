# MiningGame

**プロジェクト概要**

MiningGameは、Minecraft上でJavaとMySQLを使用して構築された鉱石採掘ゲームです。60秒間の制限時間内で鉱石を採掘し、獲得したスコアをMySQLデータベースに記録・管理します。ゲームロジックとデータ管理の設計に注力し、プレイヤーごとのスコア管理、インベントリの保存・復元、ランダムな鉱石生成、スコアリングの仕組みを実装しています。

## デモ動画
[デモ動画を再生する](https://github.com/user-attachments/assets/88007f69-c0a1-44ed-bf8a-41ad14110c11)


## ディレクトリ構成
- plugin.mininggame
   - Main.java                      : プラグインのエントリーポイント
   - command/
     - BaseCommand.java             : コマンドの基底クラス
     - MiningGameCommand.java       : ゲームのロジックとコマンド処理
   - data/
     - Player.java                  : プレイヤーのデータオブジェクト
   - mapper/
     - PlayerScoreMapper.java       : MyBatisのマッパーインターフェース
   - PlayerScoreData.java           : データベース接続およびスコア管理
   - resources/
     - mybatis-config.xml           : MyBatisの設定ファイル


# 使用技術
![Java](https://img.shields.io/badge/Language-Java-007396?logo=java&logoColor=white)
![MySQL](https://img.shields.io/badge/Database-MySQL-4479A1?logo=mysql&logoColor=white)
![MyBatis](https://img.shields.io/badge/ORM-MyBatis-000000?logo=mybatis&logoColor=white)
![Spigot](https://img.shields.io/badge/Framework-Spigot_API-FFB13B?logo=minecraft&logoColor=white)
![Git](https://img.shields.io/badge/Version%20Control-Git-F05032?logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/Repo-GitHub-181717?logo=github&logoColor=white)

## 機能概要

**ゲームコマンドの実装**
- コマンド実行の基底クラス (BaseCommand)
  コマンド実行者がプレイヤーかNPCかで処理を分ける抽象クラスを導入。これにより、コマンド処理の拡張性を高め、将来的な機能追加を容易にしています。

- メインゲームロジック (MiningGameCommand)
  /mining gameコマンドでゲームを開始し、鉱石の生成、スコア管理、インベントリの保存・復元を行います。

## データベース接続とスコア管理
- MyBatisを用いたデータアクセス:
  PlayerScoreDataクラスを使用して、MySQLデータベースと接続し、プレイヤーのスコアデータを永続化しています。
  
**ER図**
- スコア管理のためのデータベース構造は以下の通りです。

```sql
+----------------+
|  player_score  |
+----------------+
| id             | INT (PK, AUTO_INCREMENT) |
| player_name    | VARCHAR(255)              |
| score          | INT                       |
| registered_at  | DATETIME                  |
+----------------+
```

**MyBatis設定ファイル**
- MyBatisの設定ファイルは以下の通りです：
```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/spigot_server"/>
                <property name="username" value="root"/>
                <property name="password" value="rootroot"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper class="plugin.mininggame.mapper.PlayerScoreMapper"/>
    </mappers>
</configuration>
```

## コア機能と技術的工夫
**インベントリの保存と復元**
- 機能概要:
  ゲーム開始前にプレイヤーのインベントリと装備状態を保存し、終了後に元の状態を復元します。これにより、ゲーム中のインベントリ変化が影響しないように設計しています。
- コード例:
```java
private void savePlayerInventory(org.bukkit.entity.Player player) {
    playerInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());
    playerArmorInventories.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());
    player.getInventory().clear();
}

private void restorePlayerInventory(org.bukkit.entity.Player player) {
    UUID playerUUID = player.getUniqueId();
    if (playerInventories.containsKey(playerUUID)) {
        ItemStack[] savedInventory = playerInventories.get(playerUUID);
        for (int i = 0; i < savedInventory.length; i++) {
            if (player.getInventory().getItem(i) == null || Objects.requireNonNull(player.getInventory().getItem(i)).getType() == Material.AIR) {
                player.getInventory().setItem(i, savedInventory[i]);
            }
        }
        playerInventories.remove(playerUUID);
    }
    if (playerArmorInventories.containsKey(playerUUID)) {
        player.getInventory().setArmorContents(playerArmorInventories.get(playerUUID));
        playerArmorInventories.remove(playerUUID);
    }
}
```

## 鉱石のランダム配置
- 機能概要:
  プレイヤーの5ブロック前方にランダムな鉱石を5×5×5の範囲で生成し、確率によって異なる鉱石が出現する仕組みを実装しています。

- コード例:
```java
private void deployOres(org.bukkit.entity.Player player, World world) {
    Location startLocation = player.getLocation().clone().add(player.getDirection().normalize().multiply(5));
    SplittableRandom random = new SplittableRandom();
    for (int x = 0; x < 5; x++) {
        for (int z = 0; z < 5; z++) {
            for (int y = 0; y < 5; y++) {
                int chance = random.nextInt(100);
                Material oreType = getMaterial(chance);
                Location oreLocation = startLocation.clone().add(x, y + 1, z);
                world.getBlockAt(oreLocation).setType(oreType);
                oreLocations.add(oreLocation);
            }
        }
    }
}
```

## スコアリング
- 機能概要:
  鉱石の種類に応じてスコアを設定し、採掘した鉱石ごとにプレイヤーのスコアを加算します。

- コード例:
```java
private int getOreScore(Material material) {
    return switch (material) {
        case STONE -> 1;
        case IRON_ORE -> 5;
        case COPPER_ORE -> 10;
        case LAPIS_ORE -> 15;
        case REDSTONE_ORE -> 20;
        case EMERALD_ORE -> 30;
        case GOLD_ORE -> 40;
        case DIAMOND_ORE -> 50;
        default -> 0;
    };
}
```
# まとめ
MiningGameは、Javaを用いた高度なオブジェクト指向設計、MyBatisを利用したデータベースアクセスの実装、Spigot APIによるMinecraftプラグイン開発を通じて、ゲームロジックとデータ永続化を効率的に管理するプロジェクトです。データベース接続の工夫、インベントリの保存・復元、ランダムな鉱石配置、スコアリングロジックなどを通じて、拡張性とスケーラビリティを考慮した設計を実現しています。

