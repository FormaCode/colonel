/*
 * MIT License
 *
 * Copyright (c) 2018 FormaCode
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.formacode.colonel.spigot;

import org.formacode.colonel.Colonel;

import java.lang.reflect.Field;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpigotColonel implements Colonel<JavaPlugin>, CommandExecutor, TabCompleter
{
	private final JavaPlugin owningPlugin;
	private CommandMap commandMap;

	public SpigotColonel(JavaPlugin owningPlugin)
	{
		this.owningPlugin = owningPlugin;
		this.commandMap = this.getCommandMap();
		if (this.commandMap == null)
		{
			throw new NullPointerException("commandMap");
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments)
	{
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] arguments)
	{
		return null;
	}

	@Override
	public JavaPlugin getOwningPlugin()
	{
		return this.owningPlugin;
	}

	public CommandMap getCommandMap()
	{
		if (this.commandMap == null)
		{
			PluginManager pluginManager = this.owningPlugin.getServer().getPluginManager();
			if (pluginManager instanceof SimplePluginManager)
			{
				SimplePluginManager simplePluginManager = (SimplePluginManager) pluginManager;
				try
				{
					Field field = SimplePluginManager.class.getDeclaredField("commandMap");
					field.setAccessible(true);
					this.commandMap = (CommandMap) field.get(simplePluginManager);
				}
				catch (IllegalAccessException | NoSuchFieldException exception)
				{
					throw new RuntimeException("Unable to set up the commandMap", exception);
				}
			}
		}
		return this.commandMap;
	}
}