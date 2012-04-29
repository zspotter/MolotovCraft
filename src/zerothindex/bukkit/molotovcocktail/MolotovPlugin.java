package zerothindex.bukkit.molotovcocktail;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MolotovPlugin extends JavaPlugin {
	
	public int radius = 2;
	public int rays = 25;
	public boolean debug = false;
	public boolean critDamage = false;
	
	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new EventListener(this), this);
	}
	
	 public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		 if (sender.isOp() && cmd.getName().equalsIgnoreCase("molotov")) {
			 if (args.length == 3) {
				 if (args[0].equalsIgnoreCase("set")) {
					 if (args[1].toLowerCase().startsWith("rad")) {
						 try {
							 radius = Integer.parseInt(args[2]);
							 sender.sendMessage(ChatColor.YELLOW+"Molotov radius set to "+radius);
							 return true;
						 } catch (NumberFormatException ex) {
							 sender.sendMessage(ChatColor.RED+"ERROR: Integer expected in arg 3");
							 return true;
						 }
					 } else if (args[1].toLowerCase().startsWith("ray")) {
						 try {
							 rays = Integer.parseInt(args[2]);
							 sender.sendMessage(ChatColor.YELLOW+"Molotov number of rays set to "+rays);
							 return true;
						 } catch (NumberFormatException ex) {
							 sender.sendMessage(ChatColor.RED+"ERROR: Integer expected in arg 3");
							 return true;
						 }
					 }
				 }
			 } else {
				 sender.sendMessage(ChatColor.YELLOW+"Molotov Cocktail Commands:");
				 sender.sendMessage(ChatColor.YELLOW+"  /molotov set rad[ius] <int>");
				 sender.sendMessage(ChatColor.YELLOW+"    Sets the fire splash radius");
				 sender.sendMessage(ChatColor.YELLOW+"  /molotov set ray[s] <int>");
				 sender.sendMessage(ChatColor.YELLOW+"    Sets the number of rays to cast");
				 return true;
			 }
		 }
		 return false;
	 }
	
}
