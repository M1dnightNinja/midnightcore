package org.wallentines.mcore;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.wallentines.mcore.util.TestUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TestCommandExecutor extends BukkitCommand {

    public TestCommandExecutor() {
        super("mcoretest");
        this.description = "MidnightCore test command";
        this.setPermission(MidnightCoreAPI.MOD_ID + ".command.test");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if(!sender.hasPermission(MidnightCoreAPI.MOD_ID + ".command.test")) {
            return false;
        }

        executeTest(sender, args);
        return true;
    }

    private void executeTest(CommandSender sender, String[] args) {

        if(!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use that command!");
            return;
        }

        SpigotPlayer player = new SpigotPlayer(Server.RUNNING_SERVER.get(), (Player) sender);

        if(args.length == 0) {
            TestUtil.cmd(player);
        }
        else if(args.length == 1) {
            switch (args[0]) {
                case "gui" -> TestUtil.guiCmd(player);
                case "equip" -> TestUtil.equipCmd(player);
                case "scoreboard" -> TestUtil.scoreboardCmd(player);
                case "skin" -> TestUtil.skinCmd(player);
                case "save" -> TestUtil.saveCmd(player);
                case "load" -> TestUtil.loadCmd(player);
            }
        } else if(args.length == 2) {
            if(args[0].equals("gui")) {
                if (args[1].equals("paged")) {
                    TestUtil.pagedGuiCmd(player);
                } else if(args[1].equals("give")) {
                    TestUtil.giveGuiCmd(player);
                }
            }
        }
    }


    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        List<String> options = getTestSubcommands(args);

        if(args.length == 0) {
            return options;
        }
        else if(args.length == 1) {
            return options.stream().filter(arg -> arg.startsWith(args[args.length - 1])).collect(Collectors.toList());
        }
        else if(args.length == 2) {
            if(args[0].equals("gui") && "paged".startsWith(args[1])) {
                return Arrays.asList("paged");
            }
        }
        return Collections.emptyList();
    }


    private List<String> getTestSubcommands(String[] args) {

        if(args.length > 1) return new ArrayList<>();
        return Arrays.asList("gui", "equip", "scoreboard", "skin", "save", "load");
    }
}
