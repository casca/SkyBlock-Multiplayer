package me.lukas.skyblockmultiplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyblockMultiplayer extends JavaPlugin {
	PluginDescriptionFile pluginFile;
	Logger log;
	PluginManager manager;

	PlayerBreackBlockListener breakBlock;
	PlayerPlaceBlockListener placeBlock;
	PlayerUseBucketListener useBucket;
	EntityDeath deathListener;

	private static World worldIslands = null;
	private static String WORLD_NAME = "skyblockislands";

	FileConfiguration config;
	File file;

	static FileConfiguration sconfig;
	static File sfile;

	String pName;
	String pNameChat;

	@Override
	public void onDisable() {
		this.log.info(this.pName + "v" + pluginFile.getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {

		this.pluginFile = this.getDescription();
		this.log = Logger.getLogger("Minecraft");

		this.pName = "[" + this.pluginFile.getName() + "] ";
		this.pNameChat = ChatColor.WHITE + "[" + ChatColor.GREEN + this.pluginFile.getName() + ChatColor.WHITE + "] ";
		this.log.info(this.pName + "v" + pluginFile.getVersion() + " enabled.");

		this.breakBlock = new PlayerBreackBlockListener(this);
		this.placeBlock = new PlayerPlaceBlockListener(this);
		this.useBucket = new PlayerUseBucketListener(this);
		this.deathListener = new EntityDeath(this);
		this.manager = this.getServer().getPluginManager();

		this.manager.registerEvent(Type.BLOCK_BREAK, breakBlock, Priority.Normal, this);
		this.manager.registerEvent(Type.BLOCK_PLACE, placeBlock, Priority.Normal, this);
		this.manager.registerEvent(Type.PLAYER_BUCKET_EMPTY, useBucket, Priority.Normal, this);
		this.manager.registerEvent(Type.ENTITY_DEATH, this.deathListener, Priority.Normal, this);

		this.config = this.getConfig();
		SkyblockMultiplayer.sconfig = this.getConfig();
		this.file = new File(this.getDataFolder(), "config.yml");
		SkyblockMultiplayer.sfile = new File(this.getDataFolder(), "config.yml");

	}

	public void loadConfig() {
		ArrayList<ItemStack> alitemsChest = new ArrayList<ItemStack>();
		alitemsChest.add(new ItemStack(Material.ICE, 2));
		alitemsChest.add(new ItemStack(Material.SAPLING, 5));
		alitemsChest.add(new ItemStack(Material.MELON, 3));
		alitemsChest.add(new ItemStack(Material.CACTUS, 1));
		alitemsChest.add(new ItemStack(Material.LAVA_BUCKET, 1));
		alitemsChest.add(new ItemStack(Material.PUMPKIN, 1));

		ItemStack[] itemsChest = new ItemStack[alitemsChest.size()];

		for (int i = 0; i < itemsChest.length; i++) {
			itemsChest[i] = alitemsChest.get(i);
		}

		String items = "";
		for (ItemStack i : alitemsChest) {
			items += i.getType().getId() + ":" + i.getAmount() + ";";
		}

		if (!this.file.exists()) {

			Data.ISLAND_DISTANCE = 50;
			Data.ITEMSCHEST = itemsChest;
			Data.SKYBLOCK_ONLINE = true;

			this.setStringbyPath("islandDistance", "50");
			this.setStringbyPath("chest.items", items);
			this.setStringbyPath("skyblockonline", "true");
		} else {
			try {
				this.config.load(this.file);
			} catch (IOException | InvalidConfigurationException e) {
				e.printStackTrace();
			}

			Data.ISLAND_DISTANCE = Integer.parseInt(this.getStringbyPath("islandDistance", "50"));

			String[] dataItems = this.getStringbyPath("chest.items", items).split(";");
			if (alitemsChest != null) {
				alitemsChest.clear();
			}
			alitemsChest = new ArrayList<ItemStack>();

			for (String s : dataItems) {
				if (s.trim() != "") {
					String[] dataValues = s.split(":");
					try {
						alitemsChest.add(new ItemStack(Integer.parseInt(dataValues[0]), Integer.parseInt(dataValues[1])));
					} catch (Exception ex) {
					}
				}
			}

			itemsChest = new ItemStack[alitemsChest.size()];

			for (int i = 0; i < itemsChest.length; i++) {
				itemsChest[i] = alitemsChest.get(i);
			}

			Data.ITEMSCHEST = itemsChest;
			Data.SKYBLOCK_ONLINE = Boolean.parseBoolean(this.getStringbyPath("skyblockonline", "true"));
		}

		SkyblockMultiplayer.getWorldIslands();
	}

	public void setStringbyPath(String path, String content) {
		this.config.set(path, content);
		try {
			this.config.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getStringbyPath(String path, String stdContent) {
		if (!this.config.contains(path)) {
			this.setStringbyPath(path, stdContent);
			return stdContent;
		}
		return this.config.getString(path);
	}

	public static World getWorldIslands() {
		if (worldIslands == null) {
			worldIslands = WorldCreator.name(SkyblockMultiplayer.WORLD_NAME).environment(Environment.NORMAL).generator(new SkyblockChunkGenerator()).createWorld();
			SkyblockMultiplayer.CreateSpawnTower();
			worldIslands.setSpawnLocation(1, SkyblockMultiplayer.getWorldIslands().getHighestBlockYAt(1, 1), 1);
		}

		return worldIslands;
	}

	@Override
	public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
		return new SkyblockChunkGenerator();
	}

	private boolean checkIfPlayerInventoryEmpty(Player p) {
		for (ItemStack i : p.getInventory().getContents()) {
			if (i != null) {
				return false;
			}
		}

		if (!p.getInventory().getHelmet().getType().equals(Material.AIR) || !p.getInventory().getChestplate().getType().equals(Material.AIR) || !p.getInventory().getLeggings().getType().equals(Material.AIR) || !p.getInventory().getBoots().getType().equals(Material.AIR)) {
			return false;
		}

		return true;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			if (cmd.getName().equalsIgnoreCase("skyblock")) {
				if (args.length == 0) {
					return true;
				}

				if (args[0].equalsIgnoreCase("offline")) {
					return this.setSkyblockOffline(sender);
				}

				if (args[0].equalsIgnoreCase("online")) {
					return this.setSkyblockOnline(sender);
				}

				if (args[0].equalsIgnoreCase("reset")) {
					return this.resetSkyblock(sender);
				}
				if (args[0].equalsIgnoreCase("reloadconfig")) {
					return this.reloadConfig(sender);
				}

				sender.sendMessage(this.pName + "Da ist kein Argument mit diesen Namen.");
				return true;
			}
			return false;
		}

		Player player = (Player) sender;

		if (cmd.getName().equalsIgnoreCase("skyblock")) {
			if (args.length == 0) {
				if (!Data.SKYBLOCK_ONLINE) {
					player.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock ist offline.");
					return true;
				}

				if (player.getWorld().equals(SkyblockMultiplayer.getWorldIslands())) {
					return true;
				}

				int playerNr = this.findPlayer(player.getName()); // Suche Spieler
				if (playerNr == -1) { // Falls der Spieler nicht in der Liste ist, f�ge ihn hinzu
					Data.PLAYERS.add(new PlayerInfo(player));
				} else {
					//Refreshe OldLocation vom Spieler
					Data.PLAYERS.get(playerNr).setOldPlayerLocation(player.getLocation());
				}
				player.teleport(SkyblockMultiplayer.getWorldIslands().getSpawnLocation()); // Teleportiere Spieler zum Spawntower in der Mitte
				player.sendMessage(this.pNameChat + ChatColor.AQUA + "Willkommen auf der Welt SkyBlock f�r Multiplayer! Es gibt momentan " + Data.ISLAND_NUMBER + " Inseln und " + Data.PLAYERS_NUMBER + " Spieler. Gib '/skyblock start' ein um eine eigene Insel zu bekommen.");
				return true;
			}

			if (args[0].equalsIgnoreCase("start")) {
				if (!Data.SKYBLOCK_ONLINE) {
					player.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock ist offline.");
					return true;
				}

				//Wenn Spieler nicht in Welt Skyblock: return true
				if (!(player.getWorld().equals(SkyblockMultiplayer.getWorldIslands()))) {
					return true;
				}

				boolean ismepty = this.checkIfPlayerInventoryEmpty(player);
				if (!ismepty) {
					player.sendMessage(this.pNameChat + ChatColor.RED + "Es ist nicht erlaubt mit Inhalt im Inventar mitzuspielen!");
					return true;
				}

				int playerNr = this.findPlayer(player.getName()); // Suche Spieler
				if (playerNr == -1) { // Falls der Spieler nicht in der Liste ist, f�ge ihn hinzu
					Data.PLAYERS.add(new PlayerInfo(player));

					playerNr = this.findPlayer(player.getName());
				}
				if (Data.PLAYERS.get(playerNr).getHasIsland()) { // Hat bereits eine Insel
					if (Data.PLAYERS.get(playerNr).getDead()) {
						player.sendMessage(this.pNameChat + ChatColor.RED + "Du hattest bereits eine Insel und hast Mist gebaut - heul den Eventmanager voll, vielleicht gibt er dir eine neue Insel.");
						return true;
					}
					// Spieler teleportieren
					player.teleport(Data.PLAYERS.get(playerNr).getIslandLocation());
					player.sendMessage(this.pNameChat + ChatColor.AQUA + "Willkommen zur�ck " + player.getName() + ".");
					return true;
				} else {
					// Erstelle eine neue Insel f�r einen neuen Spieler
					CreateNewIsland isl = new CreateNewIsland(player);
					Data.PLAYERS.get(playerNr).setIslandLocation(isl.Islandlocation);
					Data.PLAYERS.get(playerNr).setHasIslandToTrue();
					Data.PLAYERS_NUMBER++;

					// Nachricht an alle
					for (PlayerInfo pinfo : Data.PLAYERS) {
						pinfo.getPlayer().sendMessage(this.pNameChat + ChatColor.AQUA + "Der Spieler " + player.getName() + " spielt mit. Damit sind es " + Data.PLAYERS_NUMBER + " Spieler.");
					}
					player.sendMessage(this.pNameChat + ChatColor.AQUA + "Fall nicht runter, und mach kein Obsidian :-)");

					return true;
				}
			}

			if (args[0].equalsIgnoreCase("leave")) {
				if (!player.getWorld().equals(SkyblockMultiplayer.getWorldIslands())) {
					return true;
				}

				int playerNr = this.findPlayer(player.getName());
				if (playerNr == -1) {
					player.setHealth(0);
					return true;
				}

				boolean ismepty = this.checkIfPlayerInventoryEmpty(player);
				if (!ismepty) {
					if (Data.PLAYERS.get(playerNr).getHasIsland()) {
						player.sendMessage(this.pNameChat + ChatColor.RED + "Es ist nicht erlaubt mit Inhalt im Inventar diese Welt zu verlassen. Packe dein Inhalt im Inventar in eine Kiste oder stirb.");
						return true;
					}
				}

				Location l = Data.PLAYERS.get(playerNr).getOldPlayerLocation();
				player.teleport(l);
				player.sendMessage(this.pNameChat + ChatColor.AQUA + "Du hast die Welt Skyblock verlassen und bist (vielleicht) zur�ck auf sicherem Boden. Achte auf den Creeper hinter dir :-)");
				return true;
			}

			if (args[0].equalsIgnoreCase("newisland")) {
				if (player.hasPermission("skyblock.newisland")) {
					if (!Data.SKYBLOCK_ONLINE) {
						player.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock ist offline.");
						return true;
					}

					if (args.length == 1) {
						player.sendMessage(this.pNameChat + ChatColor.WHITE + "Du musst einen Spielernamen angeben!");
						return true;
					}

					int playerNr = this.findPlayer(args[1]);
					if (playerNr == -1) {
						player.sendMessage(this.pNameChat + ChatColor.WHITE + "Es gibt keinen Spieler mit diesem Namen.");
						return true;
					}
					Player tp = Data.PLAYERS.get(playerNr).getPlayer();
					Data.PLAYERS.get(playerNr).setDeadToFalse();
					Data.PLAYERS.get(playerNr).setHasIslandToFalse();
					Data.PLAYERS_NUMBER--;
					player.sendMessage(this.pNameChat + ChatColor.AQUA + "Der Spieler " + tp.getName() + " hat eine neue Insel bekommen.");
					Data.PLAYERS.get(playerNr).getPlayer().sendMessage(ChatColor.GREEN + "Du hast eine neue Insel von EventManger " + player.getName() + " bekommen. Benutze '/skyblock start' um dorthin zu kommen.");
					return true;
				} else {
					player.sendMessage(this.pName + ChatColor.RED + "Du bist nicht autorisiert!");
					return true;
				}
			}

			if (args[0].equalsIgnoreCase("offline")) {
				return this.setSkyblockOffline(player);
			}

			if (args[0].equalsIgnoreCase("online")) {
				return this.setSkyblockOnline(player);
			}

			if (args[0].equalsIgnoreCase("help")) {
				for (String s : this.getListCommands().split("\n")) {
					player.sendMessage(ChatColor.AQUA + s);
				}
				return true;
			}

			if (args[0].equalsIgnoreCase("reset")) {
				return this.resetSkyblock(sender);
			}

			if (args[0].equalsIgnoreCase("reloadconfig")) {
				return this.reloadConfig(sender);
			}

			player.sendMessage(this.pNameChat + ChatColor.RED + "Da ist kein Argument mit diesen Namen.");
			return true;
		}

		return false;
	}

	public boolean setSkyblockOffline(CommandSender sender) {
		if (sender.hasPermission("skyblock.offline")) {
			try {
				if (sender instanceof Player) {
					sender.sendMessage(this.pNameChat + ChatColor.WHITE + "Stoppe Skyblock...");
				} else {
					sender.sendMessage(this.pName + "Stoppe Skyblock...");
				}

				//Checke Spieler ob keiner mehr in Welt Skyblock ist					
				Player[] playerList = this.getServer().getOnlinePlayers();
				for (Player p : playerList) {
					if (p.getWorld().equals(SkyblockMultiplayer.getWorldIslands())) {
						if (sender instanceof Player) {
							sender.sendMessage(this.pNameChat + ChatColor.RED + "Es sind Spieler in der Welt Skyblock. Skyblock kann nicht deaktviert werden!");
						} else {
							sender.sendMessage(this.pName + "Es sind Spieler in der Welt Skyblock. Skyblock kann nicht deaktviert werden!");
						}
						return true;
					}
				}

				this.getServer().unloadWorld(SkyblockMultiplayer.WORLD_NAME, true);
				SkyblockMultiplayer.worldIslands = null;
				Data.SKYBLOCK_ONLINE = false;
				Data.setStatus(false);

				// Resete die Spielerinfo
				Data.ISLAND_NUMBER = 0;
				Data.PLAYERS.clear();
				Data.PLAYERS_NUMBER = 0;

				if (sender instanceof Player) {
					sender.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock f�r Multiplayer ist nun offline. Um die Welt zu resten, l�sche den Ordner " + SkyblockMultiplayer.WORLD_NAME);
				} else {
					sender.sendMessage(this.pName + "Skyblock f�r Multiplayer ist nun offline. Um die Welt zu resten, l�sche den Ordner " + SkyblockMultiplayer.WORLD_NAME);
				}
				return true;

			} catch (Exception ex) {
				System.out.println(ex.getMessage());
				if (sender instanceof Player) {
					sender.sendMessage(this.pNameChat + ChatColor.RED + "Error orccured! Error-Message posted in Server Konsole.");
				} else {
					sender.sendMessage(this.pName + "Error orccured! Error-Message posted in Server Konsole.");
				}
				return true;
			}
		} else {
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.RED + "Du bist nicht autorisiert!");
			} else {
				sender.sendMessage(this.pName + "Du bist nicht autorisiert!");
			}
			return true;
		}
	}

	public boolean setSkyblockOnline(CommandSender sender) {
		if (sender.hasPermission("skyblock.online")) {
			if (sender instanceof Player) {
				sender.sendMessage("Starte Skyblock...");
			} else {
				sender.sendMessage("Starte Skyblock...");
			}
			SkyblockMultiplayer.worldIslands = null;
			SkyblockMultiplayer.getWorldIslands();
			Data.SKYBLOCK_ONLINE = true;
			Data.setStatus(true);

			// Resete die Spielerinfo
			Data.ISLAND_NUMBER = 0;
			Data.PLAYERS.clear();
			Data.PLAYERS_NUMBER = 0;
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock f�r Multiplayer ist nun online!");
			} else {
				sender.sendMessage(this.pName + "Skyblock f�r Multiplayer ist nun online!");
			}
			return true;
		} else {
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.RED + "Du bist nicht autorisiert!");
			} else {
				sender.sendMessage(this.pName + "Du bist nicht autorisiert!");
			}
			return true;
		}
	}

	public boolean resetSkyblock(CommandSender sender) {
		if (!sender.hasPermission("skyblock.reset")) {
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.RED + "Du bist nicht autorisiert!");
			} else {
				sender.sendMessage(this.pName + "Du bist nicht autorisiert!");
			}
			return true;
		}

		if (Data.SKYBLOCK_ONLINE) {
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.RED + "Skyblock muss offline sein bevor, du es reseten kannst!");
			} else {
				sender.sendMessage(this.pName + "Skyblock muss offline sein bevor, du es reseten kannst!");
			}
			return true;
		}

		if (sender instanceof Player) {
			sender.sendMessage(this.pNameChat + ChatColor.AQUA + "Reseting Skyblock...");
		} else {
			sender.sendMessage(this.pName + "Reseting Skyblock...");
		}

		this.getServer().unloadWorld(SkyblockMultiplayer.WORLD_NAME, true);

		//Get Files and delete them
		this.sfiles = new ArrayList<File>();
		this.getAllFilesAndDirectories(SkyblockMultiplayer.WORLD_NAME);

		for (File f : this.sfiles) {
			f.delete();
		}

		//Create Skyblock
		SkyblockMultiplayer.worldIslands = null;
		SkyblockMultiplayer.getWorldIslands();

		//Resete die Spielerinfo
		Data.ISLAND_DISTANCE = 0;
		Data.PLAYERS.clear();
		Data.PLAYERS_NUMBER = 0;

		if (sender instanceof Player) {
			sender.sendMessage(this.pNameChat + ChatColor.AQUA + "Skyblock wurde resetet.");
		} else {
			sender.sendMessage(this.pName + "Skyblock wurde resetet.");
		}
		return true;
	}

	public boolean reloadConfig(CommandSender sender) {
		if (!sender.hasPermission("skyblock.reloadconfig")) {
			if (sender instanceof Player) {
				sender.sendMessage(this.pNameChat + ChatColor.RED + "Du bist nicht autorisiert!");
			} else {
				sender.sendMessage(this.pName + "Du bist nicht autorisiert!");
			}
			return true;
		}

		this.loadConfig();
		if (sender instanceof Player) {
			sender.sendMessage(this.pNameChat + ChatColor.AQUA + "Config-Datei wurde neu geladen.");
		} else {
			sender.sendMessage(this.pName + "Config-Datei wurde neu geladen.");
		}
		return true;
	}

	public String getListCommands() {
		String rightsPlayer = "Befehle f�r Spieler:\n" + "/skyblock help - Liste alle Kommandos nach Rechten gefiltert auf\n" + "/skyblock - Skyblock f�r Mutiplayer betreten\n" + "/skyblock start - Bei Skyblock mitspielen\n" + "/skyblock leave - Zur alten Position in der alten Welt zur�ckkehren\n";

		String rightsMod = "Befehle f�r Mod:\n" + "/skyblock newisland [player] - Einem Spieler die M�glichkeit geben einer neuen Insel zu joinen\n";

		String rightsOp = "Befehle f�r Op:\n" + "/skyblock offline - Deaktiviert Skyblock, damit diese Option geht darf kein Spieler mehr in Skyblock sein und die Welt kann gel�scht werden\n" + "/skyblock online - Aktiviert Skyblock und erstellt die Welt neu, wenn nicht vorhanden";

		return rightsPlayer + "\n" + rightsMod + "\n" + rightsOp;
	}

	ArrayList<File> sfiles;

	public void getAllFilesAndDirectories(String path) {

		File dirpath = new File(path);
		if (!dirpath.exists()) {
			return;
		}

		for (File f : dirpath.listFiles()) {
			try {
				if (!f.isDirectory()) {
					this.sfiles.add(f);
				} else {
					this.getAllFilesAndDirectories(f.getAbsolutePath());
				}
			} catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
	}

	public int findPlayer(String playername) {
		for (int i = 0; i < Data.PLAYERS.size(); i++) {
			if (Data.PLAYERS.get(i).getPlayerName().equalsIgnoreCase(playername)) {
				return i;
			}
		}
		return -1;
	}

	private static void makeBlock(int x, int y, int z, Material m) {
		SkyblockMultiplayer.getWorldIslands().getBlockAt(x, y, z).setType(m);
	}

	private static void quader(int x, int y, int z, Material m) {
		if (y < 0)
			return;
		SkyblockMultiplayer.makeBlock(x, y, z, m);
		SkyblockMultiplayer.makeBlock(x, y, z + 1, m);
		SkyblockMultiplayer.makeBlock(x + 1, y, z, m);
		SkyblockMultiplayer.makeBlock(x + 1, y, z + 1, m);
	}

	private static void CreateSpawnTower() {
		int yStart = 2;
		int yEnde = 90;
		int[][] lavatreppe = { { 2, 0 }, { 2, 0 }, { 0, 2 }, { 0, 2 }, { -2, 0 }, { -2, 0 }, { 0, -2 }, { 0, -2 } };
		int i = 0;
		int x = -2;
		int z = -2;
		for (int y = yStart; y < yEnde - 2; y++) {
			// Mache Obsidian Turm
			SkyblockMultiplayer.quader(0, y, 0, Material.OBSIDIAN);
			// Mache Innenwand
			for (int xw = -2; xw < 4; xw++) {
				SkyblockMultiplayer.makeBlock(xw, y, -2, Material.GLASS);
				SkyblockMultiplayer.makeBlock(xw, y, 3, Material.GLASS);
			}
			for (int zw = -2; zw < 4; zw++) {
				SkyblockMultiplayer.makeBlock(-2, y, zw, Material.GLASS);
				SkyblockMultiplayer.makeBlock(3, y, zw, Material.GLASS);
			}
			// Mache Lavatreppe
			SkyblockMultiplayer.quader(x, y, z, Material.getMaterial(43));
			x += lavatreppe[i][0];
			z += lavatreppe[i][1];
			i++;
			if (i == lavatreppe.length)
				i = 0;

		}
		// Wassertreppe
		i = 0;
		x = -2;
		z = -2;
		for (int y = yStart; y <= yEnde; y++) {
			SkyblockMultiplayer.quader(x, y - 3, z, Material.GLASS);
			x += lavatreppe[i][0];
			z += lavatreppe[i][1];
			i++;
			if (i == lavatreppe.length)
				i = 0;
		}

		// Setze die Treppe, Ganzesteine
		i = 0;
		x = -2;
		z = -4;
		int[][] stairsWhole = { { 4, 0 }, { 2, 2 }, { 0, 4 }, { -2, 2 }, { -4, 0 }, { -2, -2 }, { 0, -4 }, { 2, -2 } };
		for (int y = yStart + 1; y < yEnde - 1; y++) {
			SkyblockMultiplayer.quader(x, y, z, Material.getMaterial(43));
			x += stairsWhole[i][0];
			z += stairsWhole[i][1];
			i++;
			if (i == stairsWhole.length)
				i = 0;
		}
		// Setze die Treppe, Halbesteine
		i = 0;
		x = -4;
		z = -4;
		int[][] stairsHalf = { { 4, 0 }, { 4, 0 }, { 0, 4 }, { 0, 4 }, { -4, 0 }, { -4, 0 }, { 0, -4 }, { 0, -4 } };
		for (int y = yStart + 1; y < yEnde - 1; y++) {
			SkyblockMultiplayer.quader(x, y, z, Material.getMaterial(44));
			x += stairsHalf[i][0];
			z += stairsHalf[i][1];
			i++;
			if (i == stairsHalf.length)
				i = 0;
		}

		// Setze Lava
		SkyblockMultiplayer.makeBlock(2, yEnde - 3, 2, Material.LAVA);
		// Setze Wasser
		SkyblockMultiplayer.makeBlock(-1, yEnde - 3, 0, Material.WATER);

		// Mache Dach
		for (x = -2; x < 4; x++) {
			for (z = -2; z < 4; z++) {
				SkyblockMultiplayer.makeBlock(x, yEnde - 2, z, Material.getMaterial(43));
			}
		}

		// Zaun
		for (x = -2; x < 4; x++) {
			SkyblockMultiplayer.makeBlock(x, yEnde - 1, -2, Material.FENCE);
		}
		for (z = -2; z < 4; z++) {
			SkyblockMultiplayer.makeBlock(-2, yEnde - 1, z, Material.FENCE);
			SkyblockMultiplayer.makeBlock(3, yEnde - 1, z, Material.FENCE);
		}

		// Fackeln
		SkyblockMultiplayer.makeBlock(-2, yEnde, -2, Material.TORCH);
		SkyblockMultiplayer.makeBlock(-2, yEnde, 3, Material.TORCH);
		SkyblockMultiplayer.makeBlock(3, yEnde, -2, Material.TORCH);
		SkyblockMultiplayer.makeBlock(3, yEnde, 3, Material.TORCH);

		// Mache den Towerboden
		for (x = -2; x < 4; x++) {
			for (z = -2; z < 4; z++) {
				for (int y = 0; y < yStart; y++) {
					SkyblockMultiplayer.makeBlock(x, y, z, Material.AIR);
				}
				SkyblockMultiplayer.makeBlock(x, yStart, z, Material.getMaterial(43));
			}
		}

		//Mache Schilder
		SkyblockMultiplayer.getWorldIslands().getBlockAt(1, yEnde - 1, 2).setType(Material.SIGN_POST);
		Sign s1 = (Sign) SkyblockMultiplayer.getWorldIslands().getBlockAt(1, yEnde - 1, 2).getState();
		s1.getBlock().setData((byte) 8);
		s1.setLine(0, "Willkommen auf");
		s1.setLine(1, "SkyBlock-");
		s1.setLine(2, "Multiplayer!");
		SkyblockMultiplayer.getWorldIslands().getBlockAt(0, yEnde - 1, 2).setType(Material.SIGN_POST);
		Sign s2 = (Sign) SkyblockMultiplayer.getWorldIslands().getBlockAt(0, yEnde - 1, 2).getState();
		s2.getBlock().setData((byte) 8);
		s2.setLine(0, "F�r weitere");
		s2.setLine(1, "Informationen");
		s2.setLine(2, "/skyblock help ein");
		s2.setLine(3, "Viel Erfolg!");
	}
}