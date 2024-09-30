package plugin.mininggame.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import plugin.mininggame.Main;
import plugin.mininggame.data.Player;
import plugin.mininggame.mapper.data.PlayerScore;
import plugin.mininggame.PlayerScoreData;

import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * 制限時間内に出現した鉱石を採掘して、スコアを獲得するゲームを起動するコマンドです。
 * スコアは鉱石によって変わり、採掘した鉱石の合計によってスコアが変動します。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 */
public class MiningGameCommand extends BaseCommand implements  Listener {

    public static final int GAME_TIME = 60;
    private final Main main;
    private final PlayerScoreData playerScoreData = new PlayerScoreData();
    private final List<Player> playerList = new ArrayList<>();
    private final List<Location> oreLocations = new ArrayList<>();
    private boolean isGameRunning = false;
    private  static final String LIST = "List";
    private final Map<UUID, ItemStack[]> playerInventories = new HashMap<>();
    private final Map<UUID, ItemStack[]> playerArmorInventories = new HashMap<>();


    public MiningGameCommand(Main main) {
        this.main = main;
    }


    @Override
    public boolean onExecutePlayerCommand(org.bukkit.entity.Player player, Command command, String label, String[] args) {
        //最初の引数が「list」だったらスコア一覧表示して処理を終了する
        if (args.length == 1 && LIST.equalsIgnoreCase(args[0])) {
            sendPlayerScoreList(player);
            return false;
        }

        Player nowPlayer = getPlayerScore(player);

        initPlayerStatus(player);

        deployOres(player, player.getWorld());

        gamePlay(player, nowPlayer);
        return true;
    }

    @Override
    public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /**
     * 現在登録されているスコアの一覧をメッセージに送る
     * @param player　プレイヤー
     */
    private void sendPlayerScoreList(org.bukkit.entity.Player player) {
        List<PlayerScore> playerScoreList = playerScoreData.selectList();
        for (PlayerScore playerScore : playerScoreList) {
            player.sendMessage(playerScore.getId() + " | "
                    + playerScore.getPlayerName() + " | "
                    + playerScore.getScore() + " | "
                    + playerScore.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }


    /**
     * ゲームを始める前の状態を設定する
     * 体力と空腹値を最大にして、ネザライトのピッケルを装備する
     * @param player　コマンドを実行したプレイヤー
     */
    private void initPlayerStatus(org.bukkit.entity.Player player) {


        // 体力と空腹値を最大にする
        player.setHealth(20);
        player.setFoodLevel(20);

        //インベントリと装備を保存する
        savePlayerInventory(player);

        // ネザライトのピッケルを装備させる
        PlayerInventory inventory = player.getInventory();
        inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_PICKAXE));
    }


    /**
     * 現在実行しているプレイヤーのスコア情報を取得する
     * @param player　コマンドを実行したプレイヤー
     * &#064;return　現在実行しているプレイヤーのスコア情報
     */
    private Player getPlayerScore(org.bukkit.entity.Player player) {
        Player executingPlayer = playerList.stream()
                .filter(ps -> ps.getPlayerName().equals(player.getName()))  // プレイヤー名でフィルタリング
                .findFirst()
                .orElseGet(() -> addNewPlayer(player));  // 見つからなければ新規プレイヤーを追加

        executingPlayer.setGameTime(GAME_TIME);
        executingPlayer.setScore(0);
        // すべてのポーション効果を削除したい場合
        removePotionEffect(player, new ArrayList<>());

        // 特定のポーション効果のみを削除したい場合（例: スピード効果のみを削除）
        //removePotionEffect(player, Arrays.asList(PotionEffectType.SPEED));
        return executingPlayer;
    }


    /**
     * 新規のプレイヤー情報をリストに追加します
     *
     * @param player　コマンドを実行したプレイヤー
     * @return 新規プレイヤー
     */
    private Player addNewPlayer(org.bukkit.entity.Player player) {
        Player newPlayer = new Player(player.getName());
        playerList.add(newPlayer);
        return newPlayer;
    }


    /**
     * プレイヤーの前方に5×5×5の範囲でランダムに鉱石ブロックを配置します
     * プレイヤーの現在の位置と向いている方向を下にして、5ブロック前方に鉱石ブロックをランダムに配置します
     * 配置される鉱石の種類はMaterial[]oresからランダムで選ばれます
     *
     * @param player　コマンドを実行したプレイヤー
     * @param world　コマンドを実行したプレイヤーが所属するワールド
     */
    private void deployOres(org.bukkit.entity.Player player, World world) {
        Location playerLocation = player.getLocation();
        Location startLocation = playerLocation.clone();
        startLocation.setY(playerLocation.getBlockY());
        startLocation.add(playerLocation.getDirection().normalize().multiply(5));
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

    private static Material getMaterial(int chance) {
        Material oreType;
        if(chance < 2) {
            oreType = Material.DIAMOND_ORE;
        } else if (chance < 10) {
            oreType = Material.GOLD_ORE;
        } else if (chance < 20) {
            oreType = Material.EMERALD_ORE;
        } else if (chance <35) {
            oreType = Material.REDSTONE_ORE;
        } else if (chance < 50) {
            oreType = Material.LAPIS_ORE;
        } else if (chance < 70) {
            oreType = Material.COPPER_ORE;
        } else if (chance < 90) {
            oreType = Material.IRON_ORE;
        } else {
            oreType = Material.STONE;
        }
        return oreType;
    }

    /**
     * ゲームを実行します。規定の時間内に鉱石を採掘するとスコアが加算されます。合計スコアを時間経過後に表示します・
     * @param player　コマンドを実行したプレイヤー
     * @param nowPlayer　プレイヤースコア情報
     */
    private void gamePlay(org.bukkit.entity.Player player, Player nowPlayer) {
        isGameRunning = true;
        player.sendTitle("ゲーム開始", "" , 0, 30, 0);

        Bukkit.getScheduler().runTaskTimer(main, Runnable -> {
            if(nowPlayer.getGameTime() <= 0) {
                Runnable.cancel();
                isGameRunning = false;
                player.sendTitle("ゲームが終了しました。", nowPlayer.getPlayerName() + " 合計 " + nowPlayer.getScore() + "点！",
                        0,60, 0);

                removeOres(player.getWorld());
                // すべてのポーション効果を削除したい場合
                removePotionEffect(player, new ArrayList<>());

                // 特定のポーション効果のみを削除したい場合（例: スピード効果のみを削除）
                //removePotionEffect(player, Arrays.asList(PotionEffectType.SPEED));
                restorePlayerInventory(player);
                playerScoreData.insert(
                        new PlayerScore(nowPlayer.getPlayerName()
                                , nowPlayer.getScore()));

                // ゲーム終了後にプレイヤーをリストから削除する処理を追加
                removePlayerFromList(player);

                return;
            }

            if(nowPlayer.getGameTime() == 30) {
                player.sendTitle("30秒経過！", "残り半分です。", 0, 20, 0);
            }

            nowPlayer.setGameTime(nowPlayer.getGameTime() - 1);
        }, 20L, 20L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        org.bukkit.entity.Player player = e.getPlayer();
        Block block = e.getBlock();
        Material blockType = block.getType();

        if (playerList.isEmpty() || !isGameRunning) {
            return;
        }

        playerList.stream()
                .filter(p -> p.getPlayerName().equals(player.getName()))
                .findFirst()
                .ifPresent(p -> {
                    int scoreToAdd = getOreScore(blockType);
                    int newScore = p.getScore() + scoreToAdd;
                    p.setScore(newScore);
                    player.sendMessage("鉱石を破壊した！"
                            + blockType.name() + " で "
                            + scoreToAdd + " 点獲得。現在のスコアは"
                            + newScore + "点！");
                });
    }

    /**
     *
     * @param material 鉱石ごとのスコアを設定
     * &#064;return　スコア
     */
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

    /**
     *
     * @param world ゲーム開始時に出現した鉱石を終了後に消します。
     */
    private void removeOres(World world) {
        for(Location oreLocation : oreLocations) {
            Block block = world.getBlockAt(oreLocation.getBlockX(), oreLocation.getBlockY(), oreLocation.getBlockZ());
            block.setType(Material.AIR);
        }
        oreLocations.clear();
    }

    /**
     * 指定されたプレイヤーから指定された特殊効果を除外します。
     * 特定の効果だけを削除したい場合は、effects リストに削除したい効果を追加してください。
     * 空のリストを渡すとすべてのポーション効果が削除されます。
     *
     * @param player コマンドを実行したプレイヤー
     * @param effects 削除したいポーション効果のリスト
     */
    private void removePotionEffect(org.bukkit.entity.Player player, List<org.bukkit.potion.PotionEffectType> effects) {
        if (effects.isEmpty()) {
            // リストが空の場合、すべてのポーション効果を削除
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);
        } else {
            // リストに含まれるポーション効果だけを削除
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .filter(effects::contains) // 指定された効果のみ削除
                    .forEach(player::removePotionEffect);
        }
    }


    /**
     * プレイヤーのインベントリと装備スロットのアイテムを保存します。
     * ゲーム開始時に呼び出し、ゲーム終了後に元に戻すために使用します。
     * @param player　コマンドを実行したプレイヤー
     */
    private void savePlayerInventory(org.bukkit.entity.Player player) {

        playerInventories.put(player.getUniqueId(), player.getInventory().getContents().clone());

        playerArmorInventories.put(player.getUniqueId(), player.getInventory().getArmorContents().clone());

        player.getInventory().clear();
    }


    /**
     * プレイヤーのインベントリと装備スロットをゲーム開始前の状態に戻します。
     * ゲーム終了時に呼び出し、元のインベントリと装備を復元します。
     * ゲーム中に獲得したアイテムはそのまま残ります。
     * @param player　コマンドを実行したプレイヤー
     */
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
            ItemStack[] savedArmor = playerArmorInventories.get(playerUUID);
            player.getInventory().setArmorContents(savedArmor);
            playerArmorInventories.remove(playerUUID);
        }
    }

    /**
     * 指定されたプレイヤーをplayerListから削除します
     * @param player　リストから削除する対象のプレイヤー
     */
    private void removePlayerFromList(org.bukkit.entity.Player player) {
        playerList.removeIf(p -> p.getPlayerName().equals(player.getName()));
    }
}
