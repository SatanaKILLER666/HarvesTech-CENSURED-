package com.ogryzok.player.abstinence;

import com.ogryzok.manualharvest.ManualHarvestLogic;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandBiomassBurst extends CommandBase {
    @Override
    public String getName() {
        return "biomassburst";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/biomassburst <trigger|chance> [player]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(getUsage(sender));
        }

        EntityPlayerMP target = args.length >= 2 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
        ISemenStorage storage = target.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) {
            throw new CommandException("commands.harvestech.no_storage");
        }

        switch (args[0].toLowerCase()) {
            case "trigger":
                if (!ManualHarvestLogic.triggerForcedUnstableBurst(target, storage)) {
                    throw new CommandException("Cannot trigger burst: need abstinence stage 1-3, active steroid, and no active beam/harvest.");
                }
                notifyCommandListener(sender, this, "Triggered uncontrolled biomass burst for %s", target.getName());
                return;
            case "chance":
                int chance = ManualHarvestLogic.getCurrentBurstChancePercent(target, storage);
                sender.sendMessage(new TextComponentString("Current uncontrolled burst chance for " + target.getName() + ": " + chance + "%"));
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "trigger", "chance");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
