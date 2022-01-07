package dev._2lstudios.elevators;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

public enum ElevatorDirection {
    UP, DOWN;

    private static boolean contains(String[] lines, String text) {
        for (String line : lines) {
            if (line.toLowerCase().contains(text)) {
                return true;
            }
        }

        return false;
    }

    public static ElevatorDirection fromSign(Sign sign) {
        String[] lines = sign.getLines();

        return contains(lines, "[down]") ? DOWN : contains(lines, "[up]") ? UP : null;
    }

    public static ElevatorDirection fromBlock(Block block) {
        BlockState blockState = block.getState();

        if (blockState instanceof Sign) {
            return fromSign((Sign) blockState);
        }

        return null;
    }
}
