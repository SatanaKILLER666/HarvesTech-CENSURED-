package com.ogryzok.player.abstinence;

import com.ogryzok.disease.DiseaseRegistry;
import com.ogryzok.player.semen.ISemenStorage;
import com.ogryzok.player.semen.SemenProvider;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class CommandAbstinence extends CommandBase {
    @Override
    public String getName() {
        return "abstinence";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/abstinence <reset|set 0-4|ticks N> [player]";
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

        int playerArgIndex = args.length >= 3 ? 2 : (args.length >= 2 && !isInteger(args[1]) && !isKeywordValue(args[1]) ? 1 : -1);
        EntityPlayerMP target = playerArgIndex >= 0 ? getPlayer(server, sender, args[playerArgIndex]) : getCommandSenderAsPlayer(sender);
        ISemenStorage storage = target.getCapability(SemenProvider.SEMEN_CAP, null);
        if (storage == null) {
            throw new CommandException("commands.harvestech.no_storage");
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "reset":
                storage.resetAbstinence();
                storage.setAmount(storage.getCapacity());
                target.removePotionEffect(DiseaseRegistry.SEED_KEEPER);
                notifyCommandListener(sender, this, "commands.harvestech.abstinence.reset", target.getName());
                return;
            case "set":
                if (args.length < 2) throw new WrongUsageException(getUsage(sender));
                int stage = parseStage(args[1]);
                storage.setAmount(storage.getCapacity());
                storage.setAbstinenceTicks(AbstinenceData.getThresholdForStage(stage));
                storage.setAbstinenceStage(stage);
                storage.setSeedKeeper(stage >= 4);
                if (stage >= 4) {
                    target.addPotionEffect(new PotionEffect(DiseaseRegistry.SEED_KEEPER, Integer.MAX_VALUE, 0, false, false));
                } else {
                    target.removePotionEffect(DiseaseRegistry.SEED_KEEPER);
                }
                notifyCommandListener(sender, this, "commands.harvestech.abstinence.set", target.getName(), stage);
                return;
            case "ticks":
                if (args.length < 2) throw new WrongUsageException(getUsage(sender));
                int ticks = parseInt(args[1], 0);
                int stageByTicks = AbstinenceData.getStageForTicks(ticks);
                storage.setAmount(storage.getCapacity());
                storage.setAbstinenceTicks(ticks);
                storage.setAbstinenceStage(stageByTicks);
                storage.setSeedKeeper(stageByTicks >= 4);
                if (stageByTicks >= 4) {
                    target.addPotionEffect(new PotionEffect(DiseaseRegistry.SEED_KEEPER, Integer.MAX_VALUE, 0, false, false));
                } else {
                    target.removePotionEffect(DiseaseRegistry.SEED_KEEPER);
                }
                notifyCommandListener(sender, this, "commands.harvestech.abstinence.ticks", target.getName(), ticks, stageByTicks);
                return;
            default:
                throw new WrongUsageException(getUsage(sender));
        }
    }

    private int parseStage(String value) throws CommandException {
        if ("keeper".equalsIgnoreCase(value)) return 4;
        return parseInt(value, 0, 4);
    }

    private boolean isInteger(String value) {
        if (value == null || value.isEmpty()) return false;
        int start = (value.charAt(0) == '-' || value.charAt(0) == '+') ? 1 : 0;
        if (start == value.length()) return false;
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) return false;
        }
        return true;
    }

    private boolean isKeywordValue(String value) {
        return "keeper".equalsIgnoreCase(value);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "reset", "set", "ticks");
        }
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "0", "1", "2", "3", "4", "keeper");
        }
        if (args.length == 2 || args.length == 3) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
