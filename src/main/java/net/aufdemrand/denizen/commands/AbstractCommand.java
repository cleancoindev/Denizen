package net.aufdemrand.denizen.commands;

import java.rmi.activation.ActivationException;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.citizensnpcs.command.exception.CommandException;

import org.bukkit.Bukkit;

public abstract class AbstractCommand {

	public Denizen plugin = (Denizen) Bukkit.getPluginManager().getPlugin("Denizen");
	
	/* Helper methods for working with arguments */
	
	public ArgumentHelper aH = plugin.getCommandRegistry().getArgumentHelper();
	public boolean activityQueueCompatible = false;
		
	/* Activates the command class as a Denizen Command. Should be called on startup. */
	
	public void activateAs(String commandName) throws ActivationException {
	
		/* Use Bukkit to reference Denizen Plugin */
		Denizen plugin = (Denizen) Bukkit.getPluginManager().getPlugin("Denizen");
		
		/* Register command with Registry */
		if (plugin.getCommandRegistry().registerCommand(commandName, this)) return;
		else 
			throw new ActivationException("Error activating Command with Command Registry.");
	}
	
	public void activateAs(String commandName, boolean activityQueueCompatible) throws ActivationException {
		activateAs(commandName);
		this.activityQueueCompatible = activityQueueCompatible;
	}


	
	/* Execute is the method called when the Denizen Command is called from a script.
	 * If the command runs successfully, the method should return true. */

	public abstract boolean execute(ScriptEntry theEntry) throws CommandException;

}
