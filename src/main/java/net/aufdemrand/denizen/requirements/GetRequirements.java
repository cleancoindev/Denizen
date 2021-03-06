package net.aufdemrand.denizen.requirements;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.commands.core.FailCommand;
import net.aufdemrand.denizen.commands.core.FinishCommand;
import net.aufdemrand.denizen.npc.DenizenNPC;
import net.citizensnpcs.command.exception.RequirementMissingException;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GetRequirements {



	public enum RequirementMode {
		NONE, ALL, ANY
	}

	public enum Requirement {
		NONE, NAME, WEARING, ITEM, HOLDING, TIME, PRECIPITATION, ACTIVITY, FINISHED, SCRIPT, FAILED,
		STORMY, SUNNY, HUNGER, WORLD, PERMISSION, LEVEL, GROUP, MONEY, POTIONEFFECT, PRECIPITATING,
		STORMING, DURABILITY
	}


	private Denizen plugin;
	private CommandSender cs;

	public GetRequirements(Denizen denizen) {
		plugin = denizen;
	}








	public boolean check(String theScript, DenizenNPC theDenizen, LivingEntity theEntity, boolean isPlayer) throws RequirementMissingException {

		String requirementMode = plugin.getScripts().getString(theScript + ".Requirements.Mode");
		List<String> requirementList = plugin.getScripts().getStringList(theScript + ".Requirements.List");

		/* No requirements met yet, we just started! */
		int numberMet = 0; 
		boolean negativeRequirement;

		if (this.cs == null) this.cs = plugin.getServer().getConsoleSender();

		/* Requirements list null? This script is probably named wrong, or doesn't exist! */
		if (requirementList == null || requirementMode == null) {
			if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.RED + "ERROR! " + ChatColor.WHITE  + "No requirements found! This script may be named incorrectly, or simply doesn't exist! Looking for '" + theScript + ".Requirements.List'");
			return false;
		}

		/* Requirement node "NONE"? No requirements in the LIST? No need to continue, return TRUE */
		if (requirementMode.equals("NONE") || requirementList.isEmpty()) return true;

		for (String requirementEntry : requirementList) {

			/* Check if this is a Negative Requirement */
			if (requirementEntry.startsWith("-")) { 
				negativeRequirement = true; 
				requirementEntry = requirementEntry.substring(1);
			}

			else negativeRequirement = false;

			// Check requirement with RequirementRegistry

			if (plugin.getRequirementRegistry().listRequirements().containsKey(requirementEntry.split(" ")[0].toUpperCase())) {

				String[] builtArgs = null;

				if (requirementEntry.split(" ").length > 1) 
					builtArgs = plugin.getScriptEngine().helper.buildArgs(requirementEntry.split(" ", 2)[1]);

				if (!negativeRequirement) {
					if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.WHITE + "Checking Requirement '" + requirementEntry.split(" ")[0].toUpperCase() + "'");
				} else {
					if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.WHITE + "Checking NEGATIVE Requirement '" + requirementEntry.split(" ")[0].toUpperCase() + "'");
				}

				if (plugin.getRequirementRegistry().getRequirement(requirementEntry.split(" ")[0].toUpperCase()).check((Player) theEntity, theDenizen, theScript, builtArgs, negativeRequirement)) {
					numberMet++;
					if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.WHITE + "" + ChatColor.WHITE + "Requirement met!");
				}
				else {
					if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.WHITE + "" + ChatColor.WHITE + "Requirement not met!");
				}
			}

			else { // Legacy Requirements

				int temp = numberMet;

				String[] arguments = new String[25];
				String[] argumentPopulator = requirementEntry.split(" ");

				for (int count = 0; count < 25; count++) {
					if (argumentPopulator.length > count) arguments[count] = argumentPopulator[count];
					else arguments[count] = null;
				}

				try {

					switch (Requirement.valueOf(arguments[0].toUpperCase())) {

					case NONE:
						return true;

					case TIME: // (-)TIME [DAWN|DAY|DUSK|NIGHT]  or  (-)TIME [#] [#]
						if (plugin.getWorld.checkTime(theEntity.getWorld(), arguments[1], arguments[2], negativeRequirement)) numberMet++;
						break;

					case STORMING:	case STORMY:  case PRECIPITATING:  case PRECIPITATION:  // (-)PRECIPITATION
						if (plugin.getWorld.checkWeather(theEntity.getWorld(), "PRECIPITATION", negativeRequirement)) numberMet++;
						break;

					case SUNNY:  // (-)SUNNY
						if (plugin.getWorld.checkWeather(theEntity.getWorld(), "SUNNY", negativeRequirement)) numberMet++;
						break;

					case HUNGER:  // (-)HUNGER [FULL|HUNGRY|STARVING]
						if (plugin.getPlayer.checkSaturation((Player) theEntity, arguments[1], negativeRequirement)) numberMet++;
						break;

					case LEVEL:  // (-)LEVEL [#] (#)
						if (plugin.getPlayer.checkLevel((Player) theEntity, arguments[1], arguments[2], negativeRequirement)) numberMet++;
						break;

					case WORLD:  // (-)WORLD [List of Worlds]
						List<String> theWorlds = new LinkedList<String>(); // = Arrays.asList(arguments);
						for(String arg : arguments) if (arg != null) theWorlds.add(arg.toUpperCase());
						theWorlds.remove(0);   /* Remove the command from the list */
						if (plugin.getWorld.checkWorld(theEntity, theWorlds, negativeRequirement)) numberMet++;
						break;

					case NAME:  // (-)Name [List of Names]
						List<String> theNames = new LinkedList<String>(); // = Arrays.asList(arguments);
						for(String arg : arguments) if (arg != null) theNames.add(arg.toUpperCase());
						theNames.remove(0);   /* Remove the command from the list */
						if (plugin.getPlayer.checkName((Player) theEntity, theNames, negativeRequirement)) numberMet++;
						break;

					case MONEY: // (-)MONEY [# or more]
						if (plugin.getPlayer.checkFunds((Player) theEntity, arguments[1], negativeRequirement)) numberMet++;
						break;

					case ITEM: // (-)ITEM [ITEM_NAME|#:#] (# or more)
						String[] itemArgs = splitItem(arguments[1]);
						if (plugin.getPlayer.checkInventory((Player) theEntity, itemArgs[0], itemArgs[1], arguments[2], negativeRequirement)) numberMet++;
						break;

					case HOLDING: // (-)HOLDING [ITEM_NAME|#:#] (# or more)
						String[] holdingArgs = splitItem(arguments[1]);
						if (plugin.getPlayer.checkHand((Player) theEntity, holdingArgs[0], holdingArgs[1], arguments[2], negativeRequirement)) numberMet++;
						break;

					case WEARING: // (-) WEARING [ITEM_NAME|#]
						if (plugin.getPlayer.checkArmor((Player) theEntity, arguments[1], negativeRequirement)) numberMet++;
						break;

					case POTIONEFFECT: // (-)POTIONEFFECT [List of POITION_TYPESs]
						List<String> thePotions = new LinkedList<String>(); // = Arrays.asList(arguments);
						for(String arg : arguments) if (arg != null) thePotions.add(arg.toUpperCase());
						thePotions.remove(0);   /* Remove the command from the list */
						if (plugin.getPlayer.checkEffects((Player) theEntity, thePotions, negativeRequirement)) numberMet++;
						break;

					case FINISHED:
					case SCRIPT: // (-)FINISHED (#) [Script Name]
						if (plugin.getCommandRegistry().getCommand(FinishCommand.class).getScriptCompletes((Player) theEntity, requirementEntry.split(" ", 2)[1], requirementEntry.split(" ", 3)[1], negativeRequirement)) numberMet++;
						break;

					case FAILED: // (-)SCRIPT [Script Name]
						if (plugin.getCommandRegistry().getCommand(FailCommand.class).getScriptFail((Player) theEntity, requirementEntry.split(" ", 2)[1], negativeRequirement)) numberMet++;
						break;

					case GROUP:
						List<String> theGroups = new ArrayList<String>(); // = Arrays.asList(arguments);
						for(String arg : arguments) if (arg != null) theGroups.add(arg);
						theGroups.remove(0);   /* Remove the command from the list */
						if (plugin.getPlayer.checkGroups((Player) theEntity, theGroups, negativeRequirement)) numberMet++;
						break;

					case PERMISSION:  // (-)PERMISSION [this.permission.node]
						List<String> thePermissions = new LinkedList<String>(); // = Arrays.asList(arguments);
						for(String arg : arguments) if (arg != null) thePermissions.add(arg);
						thePermissions.remove(0);   /* Remove the command from the list */
						if (plugin.getPlayer.checkPermissions((Player) theEntity, thePermissions, negativeRequirement)) numberMet++;
						break;

					case DURABILITY:  // (-)DURABILITY [>,<,=] [#|#%]
						if (plugin.getPlayer.checkDurability((Player) theEntity, arguments[1], arguments[2], negativeRequirement)) numberMet++;
						break;
					}

				} catch (Throwable e) {
					if (plugin.showStackTraces) plugin.getLogger().info(e.getMessage());
					if (plugin.showStackTraces) e.printStackTrace();
					throw new RequirementMissingException(e.getMessage());
				}

				String debugger = "";

				if (!negativeRequirement) { 
					if (plugin.debugMode) debugger = debugger + "Checking LEGACY Requirement '" + requirementEntry.split(" ")[0].toUpperCase() + "'... ";
				} else {
					if (plugin.debugMode)  debugger = debugger + "Checking LEGACY NEGATIVE Requirement '" + requirementEntry.split(" ")[0].toUpperCase() + "'... ";
				}

				if (numberMet == temp) {
					if (plugin.debugMode) debugger = debugger + "requirement not met!";
				} else {
					if (plugin.debugMode) debugger = debugger + "requirement met!";
				}

				if (plugin.debugMode) cs.sendMessage(ChatColor.LIGHT_PURPLE + "| " + ChatColor.WHITE + debugger);

			}
		}

		/* Check numberMet */	
		if (requirementMode.equalsIgnoreCase("ALL") 
				&& numberMet == requirementList.size()) return true;

		String[] ModeArgs = requirementMode.split(" ");
		if (ModeArgs[0].equalsIgnoreCase("ANY")) {
			if (ModeArgs.length == 1) {
				if (numberMet >= 1) return true;
			} else {
				if (numberMet >= Integer.parseInt(ModeArgs[1])) return true;
			}
		}

		/* Nothing met, return FALSE */	
		return false;

	}



	/* 
	 * Converts a string with the format #:# (TypeId:Data) to a String[] 
	 * 
	 * Element [0] -- TypeId
	 * Element [1] -- Data
	 */

	public String[] splitItem(String theItemWithData) {

		String[] itemArgs = new String[2];
		if (theItemWithData.split(":", 2).length == 1) {
			itemArgs[0] = theItemWithData;
			itemArgs[1] = null;

		}
		else {
			itemArgs[0] = theItemWithData.split(":", 2)[0];
			itemArgs[1] = theItemWithData.split(":", 2)[1];
		}

		return itemArgs;
	}







}
