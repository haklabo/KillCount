package com.unlucky4ever.killcount.commands;

import java.sql.ResultSet;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.unlucky4ever.killcount.KillCount;
import com.unlucky4ever.killcount.extras.db.MySQL;

public class KillCountAdminCommand extends BaseCommand {
	
	public KillCount plugin;
	public FileConfiguration users;
	public HashMap<Player, Boolean> hashmap = new HashMap<Player, Boolean>();
	
	public KillCountAdminCommand(KillCount instance) {
		plugin = instance;
		permFlag = "killcount.admin";
		commandOnly = false;
		senderMustBePlayer = false;
		command.add("kca");
		helpDescription = "Admin commands for kill counts.";
	}
	
	public void perform() {
		if (parameters.size() == 0) {
			sender.sendMessage("Not enough arguments!");
		} else if (parameters.size() == 1) {
			if (parameters.get(0).equalsIgnoreCase("empty")) {
				if (sender.hasPermission("killcount.admin.empty")) {
					hashmap.put((Player) sender, true);
					sender.sendMessage(ChatColor.RED + "Are you sure you wish to empty the database?");
					sender.sendMessage(ChatColor.RED + "Type " + ChatColor.GOLD + "/kca confirm" + ChatColor.RED + " to confirm.");
				} else {
					sender.sendMessage("You lack the permissions to do this command.");
				}
			} else if (parameters.get(0).equalsIgnoreCase("confirm")) {
				if (sender.hasPermission("killcount.admin.empty")) {
					if (hashmap.containsKey((Player) sender)) {
						if (plugin.config.getString("storage-type").equalsIgnoreCase("file")) {
							if (plugin.deleteCustomConfig()) {
								hashmap.remove((Player) sender);
								sender.sendMessage(ChatColor.RED + "File deleted, please reload the config with /kca reload");
							} else {
								sender.sendMessage("There was an error while emptying the file!");
							}
						}
						if (plugin.config.getString("storage-type").equalsIgnoreCase("mysql")) {
							plugin.mysql = new MySQL(plugin.getLogger(), "[KillCount]", plugin.mysql_host, plugin.mysql_port, plugin.mysql_db, plugin.mysql_user, plugin.mysql_password);
							plugin.mysql.open();
							if (plugin.mysql.checkConnection()) {
								try {
									plugin.mysql.wipeTable("killcount");
									sender.sendMessage(ChatColor.RED + "Database emptied!");
									hashmap.remove(player.getPlayer());
								} catch (Exception e) {
									sender.sendMessage(ChatColor.RED + "There was an error wiping the database!");
									if (plugin.config.getBoolean("debug")) {
										e.printStackTrace();
									}
								}
								plugin.mysql.close();
							}
						}
					} else {
						sender.sendMessage(ChatColor.RED + "What are you confirming?");
					}
				}
			} else if (parameters.get(0).equalsIgnoreCase("reload")) {
				if (sender.hasPermission("killcount.admin.reload")) {
					plugin.reloadConfig();
					if (plugin.config.getString("storage-type").equalsIgnoreCase("file")) {
						plugin.getCustomConfig();
						plugin.saveCustomConfig();
						sender.sendMessage(ChatColor.RED + "Configuration files reloaded!");
					} else {
						sender.sendMessage(ChatColor.RED + "Configuration file reloaded!");
					}
				}
			}
		} else if (parameters.size() == 2) {
			if (parameters.get(0).equalsIgnoreCase("reset")) {
				if (sender.hasPermission("killcount.admin.reset")) {
					if (plugin.config.getString("storage-type").equalsIgnoreCase("file")) {
						users = plugin.getCustomConfig();
						if (users.isInt(parameters.get(1).toLowerCase() + ".kills")) {
							users.set(parameters.get(1).toLowerCase() + ".kills", 0);
							users.set(parameters.get(1).toLowerCase() + ".deaths", 0);
							plugin.saveCustomConfig();
							sender.sendMessage(ChatColor.RED + "Successfully reset the user + " + parameters.get(1));
						} else {
							sender.sendMessage(ChatColor.RED + "User is not in the file!");
						}
					}
					if (plugin.config.getString("storage-type").equalsIgnoreCase("mysql")) {
						plugin.mysql = new MySQL(plugin.getLogger(), "[KillCount]", plugin.mysql_host, plugin.mysql_port, plugin.mysql_db, plugin.mysql_user, plugin.mysql_password);
						plugin.mysql.open();
						if (plugin.mysql.checkConnection()) {
							try {
								ResultSet result = plugin.mysql.query("SELECT COUNT(*) FROM killcount WHERE username='" + parameters.get(1) + "'");
								result.first();
								int count = result.getInt(1);
								result.close();
								if (count == 0) {
									player.sendMessage(ChatColor.RED + parameters.get(1) + " isn't in the database!");
								} else {
									plugin.mysql.query("UPDATE killcount SET kills='0', deaths='0' WHERE username='" + parameters.get(1) + "'");
									player.sendMessage(ChatColor.RED + parameters.get(1) + " has been reset!");
								}
							} catch (Exception e) {
								if (plugin.config.getBoolean("debug")) {
									e.printStackTrace();
								}
							}
							plugin.mysql.close();
						}
					}
				}
			}
		} else {
			sender.sendMessage("You put to many arguments!");
		}
	}
}