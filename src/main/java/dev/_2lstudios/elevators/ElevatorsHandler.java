package dev._2lstudios.elevators;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import dev._2lstudios.elevators.config.MessagesConfig;

public class ElevatorsHandler {
    private MessagesConfig messagesConfig;

    public ElevatorsHandler(MessagesConfig messagesConfig) {
        this.messagesConfig = messagesConfig;
    }

    private Sign getValidSign(Block block, ElevatorDirection validDirection) {
        BlockState blockState = block.getState();

        if (blockState instanceof Sign) {
            ElevatorDirection direction = ElevatorDirection.fromBlock(block);

            if (direction != null && direction == validDirection) {
                return (Sign) blockState;
            }
        }

        return null;
    }

    public Sign getElevatorAbove(Sign sign) {
        World world = sign.getWorld();
        int x = sign.getX();
        int z = sign.getZ();

        for (int y = sign.getY(); y < world.getMaxHeight(); y++) {
            Sign targetSign = getValidSign(world.getBlockAt(x, y, z), ElevatorDirection.DOWN);

            if (targetSign != null) {
                return targetSign;
            }
        }

        return null;
    }

    public Sign getElevatorBelow(Sign sign) {
        World world = sign.getWorld();
        int x = sign.getX();
        int z = sign.getZ();

        for (int y = sign.getY(); y > 0; y--) {
            Sign targetSign = getValidSign(world.getBlockAt(x, y, z), ElevatorDirection.UP);

            if (targetSign != null) {
                return targetSign;
            }
        }

        return null;
    }

    public boolean teleport(Player player, Location location) {
        if (location.getBlock().isEmpty()) {
            location.setDirection(player.getLocation().getDirection());
            player.teleport(location.add(0.5, 0, 0.5));
            player.sendMessage(messagesConfig.getMessage("teleported"));
            return true;
        }

        return false;
    }

    public void teleport(Player player, Sign sign) {
        ElevatorDirection direction = ElevatorDirection.fromSign(sign);

        if (direction != null) {
            Sign targetSign = direction == ElevatorDirection.UP ? getElevatorAbove(sign) : getElevatorBelow(sign);

            if (targetSign != null) {
                Location targetLocation = targetSign.getLocation().clone();

                targetLocation.add(0, 1, 0);

                if (!teleport(player, targetLocation)) {
                    targetLocation.add(0, -2, 0);

                    if (!teleport(player, targetLocation)) {
                        player.sendMessage(messagesConfig.getMessage("no-space"));
                    }
                }
            } else {
                player.sendMessage(messagesConfig.getMessage("no-elevator"));
            }
        }
    }
}
