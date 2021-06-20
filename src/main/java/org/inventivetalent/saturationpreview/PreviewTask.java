package org.inventivetalent.saturationpreview;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.reflection.minecraft.Minecraft;

import static org.inventivetalent.saturationpreview.SaturationPreview.*;

public class PreviewTask extends BukkitRunnable {

    Player player;
    boolean shown = false;
    int actualLevel;
    int previewLevel;

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        sendActualPacket();
    }

    public PreviewTask(Player player, int level) {
        this.player = player;
        this.actualLevel = player.getFoodLevel();
        this.previewLevel = level;
    }

    @Override
    public void run() {
        if (shown) {
            sendActualPacket();
            shown = false;
        } else {
            sendPreviewPacket();
            shown = true;
        }

        if ((actualLevel = player.getFoodLevel()) >= 20) {
            cancel();
        }
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            cancel();
        }
        if (player.getItemInHand() == null || player.getItemInHand().getType() == Material.AIR || player.getItemInHand().getAmount() <= 0) {
            cancel();
        }
    }

    void sendActualPacket() {
        sendPacket(PacketPlayOutUpdateHealthConstructorResolver.resolveWrapper(new Class[]{
                float.class,
                int.class,
                float.class}).newInstance(CraftPlayerMethodResolver.resolveWrapper("getScaledHealth")
                .invokeSilent(player), Math.min(actualLevel, 20), player.getSaturation()));
    }

    void sendPreviewPacket() {
        sendPacket(PacketPlayOutUpdateHealthConstructorResolver.resolveWrapper(new Class[]{
                float.class,
                int.class,
                float.class}).newInstance(CraftPlayerMethodResolver.resolveWrapper("getScaledHealth")
                .invokeSilent(player), Math.min(actualLevel + previewLevel, 20), player.getSaturation()));
    }

    void sendPacket(Object packet) {
        try {
            PlayerConnectionMethodResolver.resolveWrapper("sendPacket")
                    .invoke(EntityPlayerFieldResolver.resolveAccessor("playerConnection", "b").get(Minecraft.getHandle(player)), packet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
