package eu.metacloudservice.bungee.command;

import eu.metacloudservice.CloudAPI;
import eu.metacloudservice.Driver;
import eu.metacloudservice.commands.translate.Translator;
import eu.metacloudservice.bungee.BungeeBootstrap;
import eu.metacloudservice.configuration.dummys.message.Messages;
import lombok.SneakyThrows;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CloudCommand extends Command implements TabExecutor {
    public CloudCommand(String name) {
        super(name);
    }

    @SneakyThrows
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (commandSender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) commandSender;
            Messages messages = CloudAPI.getInstance().getMessages();
            if (player.hasPermission("metacloud.command.use") || player.hasPermission("metacloud.command.*")){
                if (args.length == 0){
                    sendHelp(player);
                }else {
                    if (CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]) != null) {
                        String[] refreshedArguments = Arrays.copyOfRange(args, 1, args.length);
                        CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]).performCommand(CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]), player, null, null, refreshedArguments);
                    }else {
                        sendHelp(player);
                    }
                }
            }else {
                BungeeBootstrap.getInstance().audiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(new Translator().translate("§8▷ §7The network uses §bMetacloud§8 [§a"+Driver.getInstance().getMessageStorage().version+"§8]")));
                BungeeBootstrap.getInstance().audiences.player(player).sendMessage(MiniMessage.miniMessage().deserialize(new Translator().translate("§8▷ §fhttps://metacloudservice.eu/")));

            }
        }
    }


    public void sendHelp(ProxiedPlayer player){
        Messages messages = CloudAPI.getInstance().getMessages();
        String PREFIX = messages.getMessages().get("prefix").replace("&", "§");
        CloudAPI.getInstance().getPluginCommandDriver().getCommands().forEach(proxyCommand -> {
            player.sendMessage(PREFIX + proxyCommand.getDescription());
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            CloudAPI.getInstance().getPluginCommandDriver().getCommands().forEach(proxyCommand -> {
                suggestions.add(proxyCommand.getCommand());
            });
        } else {
            if (CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]) != null) {
                if (args.length == 2) {
                    suggestions.addAll(CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]).tabComplete(new String[]{}));
                } else {
                    String[] refreshedArguments = Arrays.copyOfRange(args, 1, args.length);
                    suggestions.addAll(CloudAPI.getInstance().getPluginCommandDriver().getCommand(args[0]).tabComplete(refreshedArguments));
                }
            }
        }
        String prefix = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(suggestion -> suggestion.toLowerCase().startsWith(prefix))
                .collect(Collectors.toList());
    }
}
