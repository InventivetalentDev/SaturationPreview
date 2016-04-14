package org.inventivetalent.saturationpreview;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.inventivetalent.pluginannotations.description.Author;
import org.inventivetalent.pluginannotations.description.Plugin;
import org.inventivetalent.reflection.resolver.ConstructorResolver;
import org.inventivetalent.reflection.resolver.FieldResolver;
import org.inventivetalent.reflection.resolver.MethodResolver;
import org.inventivetalent.reflection.resolver.minecraft.NMSClassResolver;
import org.inventivetalent.reflection.resolver.minecraft.OBCClassResolver;
import org.mcstats.MetricsLite;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(name = "SaturationPreview")
@Author("inventivetalent")
public class SaturationPreview extends JavaPlugin implements Listener {

	static NMSClassResolver nmsClassResolver = new NMSClassResolver();
	static OBCClassResolver obcClassResolver = new OBCClassResolver();

	static Class<?> ItemStack                 = nmsClassResolver.resolveSilent("ItemStack");
	static Class<?> Item                      = nmsClassResolver.resolveSilent("Item");
	static Class<?> ItemFood                  = nmsClassResolver.resolveSilent("ItemFood");
	static Class<?> EntityPlayer              = nmsClassResolver.resolveSilent("EntityPlayer");
	static Class<?> PlayerConnection          = nmsClassResolver.resolveSilent("PlayerConnection");
	static Class<?> PacketPlayOutUpdateHealth = nmsClassResolver.resolveSilent("PacketPlayOutUpdateHealth");
	static Class<?> CraftItemStack            = obcClassResolver.resolveSilent("inventory.CraftItemStack");
	static Class<?> CraftPlayer               = obcClassResolver.resolveSilent("entity.CraftPlayer");

	static MethodResolver CraftItemStackMethodResolver   = new MethodResolver(CraftItemStack);
	static MethodResolver CraftPlayerMethodResolver      = new MethodResolver(CraftPlayer);
	static MethodResolver ItemStackMethodResolver        = new MethodResolver(ItemStack);
	static MethodResolver ItemFoodMethodResolver         = new MethodResolver(ItemFood);
	static MethodResolver PlayerConnectionMethodResolver = new MethodResolver(PlayerConnection);

	static FieldResolver EntityPlayerFieldResolver = new FieldResolver(EntityPlayer);

	static ConstructorResolver PacketPlayOutUpdateHealthConstructorResolver = new ConstructorResolver(PacketPlayOutUpdateHealth);

	Map<UUID, PreviewTask> previewTaskMap = new HashMap<>();

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		try {
			MetricsLite metrics = new MetricsLite(this);
			if (metrics.start()) {
				getLogger().info("Metrics started");
			}
		} catch (Exception e) {
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void on(PlayerItemHeldEvent event) {
		if (!event.getPlayer().hasPermission("saturationpreview.use")) { return; }
		ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
		cancelTask(event.getPlayer());
		if (itemStack != null && itemStack.getAmount() > 0 && itemStack.getType() != Material.AIR) {
			Object nmsItemStack = CraftItemStackMethodResolver.resolveWrapper("asNMSCopy").invokeSilent(null, itemStack);
			Object nmsItem = ItemStackMethodResolver.resolveWrapper("getItem").invokeSilent(nmsItemStack);
			if (ItemFood.isAssignableFrom(nmsItem.getClass())) {
				startTask(event.getPlayer(), (int) ItemFoodMethodResolver.resolveWrapper("getNutrition").invokeSilent(nmsItem, nmsItemStack));
			}
		}
	}

	void startTask(Player player, int level) {
		cancelTask(player);

		PreviewTask task = new PreviewTask(player, level);
		previewTaskMap.put(player.getUniqueId(), task);
		task.runTaskTimer(this, 10, 10);
	}

	void cancelTask(Player player) {
		if (previewTaskMap.containsKey(player.getUniqueId())) {
			previewTaskMap.remove(player.getUniqueId()).cancel();
		}
	}

}
