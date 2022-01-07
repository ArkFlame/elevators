package dev._2lstudios.elevators.listeners;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import dev._2lstudios.elevators.ElevatorsHandler;

public class PlayerInteractListener implements Listener {
    private ElevatorsHandler elevatorsHandler;

    public PlayerInteractListener(ElevatorsHandler elevatorsHandler) {
        this.elevatorsHandler = elevatorsHandler;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (player == null || !player.isOnline()) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        BlockState blockState = block.getState();

        if (!(blockState instanceof Sign)) {
            return;
        }

        Sign sign = (Sign) blockState;

        elevatorsHandler.teleport(player, sign);
    }
}
