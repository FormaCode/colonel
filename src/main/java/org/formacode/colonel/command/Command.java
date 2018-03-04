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

package org.formacode.colonel.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.formacode.colonel.command.annotation.CommandExecutor;
import org.formacode.colonel.command.annotation.CommandHeader;
import org.formacode.colonel.command.annotation.DefaultCommandExecutor;
import org.formacode.colonel.util.MessageUtils;
import org.formacode.colonel.util.ReflectionUtils;

import org.bukkit.command.CommandSender;

public final class Command extends org.bukkit.command.Command
{
	private final Object executor;
	private final CommandHeader header;
	private Collection<Method> executors;
	private Method defaultExecutor;

	public Command(Object executor, CommandHeader header)
	{
		super(header.name(), header.description(), header.usage(), Arrays.asList(header.aliases()));
		this.executor = executor;
		this.header = header;
		registerExecutors();
	}

	private void registerExecutors()
	{
		Class<?> executorClass = this.executor.getClass();
		Method[] methods = executorClass.getMethods();
		this.executors = new ArrayList<>(methods.length);
		for (Method method : methods)
		{
			Optional<CommandExecutor> executorOptional = ReflectionUtils.getAnnotation(method, CommandExecutor.class);
			if (executorOptional.isPresent())
			{
				this.executors.add(method);
				continue;
			}
			Optional<DefaultCommandExecutor> defaultExecutorOptional = ReflectionUtils.getAnnotation(method, DefaultCommandExecutor.class);
			if (!defaultExecutorOptional.isPresent())
			{
				continue;
			}
			if (this.defaultExecutor != null)
			{
				throw new IllegalStateException("Executor cannot have more than one DefaultCommandExecutor");
			}
			this.defaultExecutor = method;
		}
	}

	@Override
	public boolean execute(CommandSender sender, String label, String[] arguments)
	{
		run(sender, label, arguments);
		return false;
	}

	private void run(CommandSender sender, String label, String[] arguments)
	{
		{
			String permission = this.header.permission();
			if (!permission.isEmpty() && !sender.hasPermission(permission))
			{
				String permissionMessage = this.header.permissionMessage();
				if (!permissionMessage.isEmpty())
				{
					MessageUtils.sendColoredMessage(sender, permissionMessage.replace("{PERMISSION}", permission));
				}
				return;
			}
		}
		if (arguments.length > 0)
		{
			for (Method method : this.executors)
			{
				CommandExecutor executor = method.getAnnotation(CommandExecutor.class);
				String name = executor.name();
				String[] aliases = executor.aliases();
				if (!arguments[0].equalsIgnoreCase(name) && Arrays.stream(aliases).anyMatch(alias -> arguments[0].equalsIgnoreCase(alias)))
				{
					return;
				}
				String permission = executor.permission();
				String permissionMessage = executor.permissionMessage();
				if (!permission.isEmpty() && !sender.hasPermission(permission))
				{
					if (!permissionMessage.isEmpty())
					{
						MessageUtils.sendColoredMessage(sender, permissionMessage.replace("{PERMISSION}", permission));
					}
					return;
				}
				String[] subArguments = Arrays.copyOfRange(arguments, 1, arguments.length);
				int subArgumentsLength = subArguments.length;
				int minArguments = executor.minArguments();
				int maxArguments = executor.maxArguments();
				if ((minArguments == -1 || subArgumentsLength >= minArguments) && (maxArguments == -1 || subArgumentsLength <= maxArguments))
				{
					ReflectionUtils.invoke(method, this.executor, sender, label, subArguments);
					return;
				}
				String usageMessage = executor.usageMessage();
				String usage = executor.usage();
				if (!usageMessage.isEmpty() && !usage.isEmpty())
				{
					MessageUtils.sendColoredMessage(sender, usageMessage.replace("{USAGE}", usage));
				}
			}
		}
		if (this.defaultExecutor != null)
		{
			DefaultCommandExecutor defaultExecutor = this.defaultExecutor.getAnnotation(DefaultCommandExecutor.class);
			Class<? extends CommandSender> executableBy = defaultExecutor.executableBy();
			String executableByMessage = defaultExecutor.executableByMessage();
			if (!executableBy.isAssignableFrom(sender.getClass()) && !executableByMessage.isEmpty())
			{
				MessageUtils.sendColoredMessage(sender, executableByMessage.replace("{EXECUTABLE_BY}", executableBy.getSimpleName()));
				return;
			}
			int argumentsLength = arguments.length;
			int minArguments = defaultExecutor.minArguments();
			int maxArguments = defaultExecutor.maxArguments();
			if ((minArguments == -1 || argumentsLength >= minArguments) && (maxArguments == -1 || argumentsLength <= maxArguments))
			{
				ReflectionUtils.invoke(this.defaultExecutor, this.executor, sender, label, arguments);
				return;
			}
		}
		String usageMessage = this.header.usageMessage();
		String usage = this.header.usage();
		if (!usageMessage.isEmpty() && !usage.isEmpty())
		{
			MessageUtils.sendColoredMessage(sender, usageMessage.replace("{USAGE}", usage));
		}
	}
}