package org.inventivetalent.saturationpreview;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.inventivetalent.pluginannotations.config.ConfigValue;
import org.inventivetalent.pluginannotations.description.Author;
import org.inventivetalent.pluginannotations.description.Plugin;
import org.inventivetalent.reflection.resolver.ConstructorResolver;
import org.inventivetalent.reflection.resolver.FieldResolver;
import org.inventivetalent.reflection.resolver.MethodResolver;
import org.inventivetalent.reflection.resolver.minecraft.NMSClassResolver;
import org.inventivetalent.reflection.resolver.minecraft.OBCClassResolver;

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
	static Class<?> FoodInfo                  = nmsClassResolver.resolveSilent("FoodInfo");
	static Class<?> EntityPlayer              = nmsClassResolver.resolveSilent("EntityPlayer");
	static Class<?> PlayerConnection          = nmsClassResolver.resolveSilent("PlayerConnection");
	static Class<?> PacketPlayOutUpdateHealth = nmsClassResolver.resolveSilent("PacketPlayOutUpdateHealth");
	static Class<?> CraftItemStack            = obcClassResolver.resolveSilent("inventory.CraftItemStack");
	static Class<?> CraftPlayer               = obcClassResolver.resolveSilent("entity.CraftPlayer");

	static MethodResolver CraftItemStackMethodResolver   = new MethodResolver(CraftItemStack);
	static MethodResolver CraftPlayerMethodResolver      = new MethodResolver(CraftPlayer);
	static MethodResolver ItemStackMethodResolver        = new MethodResolver(ItemStack);
	static MethodResolver ItemMethodResolver             = new MethodResolver(Item);
	static MethodResolver FoodInfoMethodResolver         = new MethodResolver(FoodInfo);
	static MethodResolver PlayerConnectionMethodResolver = new MethodResolver(PlayerConnection);

	static FieldResolver EntityPlayerFieldResolver = new FieldResolver(EntityPlayer);

	static ConstructorResolver PacketPlayOutUpdateHealthConstructorResolver = new ConstructorResolver(PacketPlayOutUpdateHealth);

	Map<UUID, PreviewTask> previewTaskMap = new HashMap<>();

	@ConfigValue(path = "applySlowness") boolean applySlowness = true;

	@Override
	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, this);

		new Metrics(this, 6505);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void on(PlayerItemHeldEvent event) {
		if (!event.getPlayer().hasPermission("saturationpreview.use")) { return; }
		ItemStack itemStack = event.getPlayer().getInventory().getItem(event.getNewSlot());
		cancelTask(event.getPlayer());
		if (itemStack != null && itemStack.getAmount() > 0 && itemStack.getType() != Material.AIR) {
			Object nmsItemStack = CraftItemStackMethodResolver.resolveWrapper("asNMSCopy").invokeSilent(null, itemStack);
			Object nmsItem = ItemStackMethodResolver.resolveWrapper("getItem").invokeSilent(nmsItemStack);
			Object foodInfo = ItemMethodResolver.resolveWrapper("getFoodInfo").invokeSilent(nmsItem);
			if (foodInfo != null) {
				startTask(event.getPlayer(), (int) FoodInfoMethodResolver.resolveWrapper("getNutrition", "a").invokeSilent(foodInfo));
			}
		}
	}

	@EventHandler
	public void on(PlayerToggleSprintEvent event) {
		if (event.getPlayer().getFoodLevel() < 6) {
			if (previewTaskMap.containsKey(event.getPlayer().getUniqueId())) {
				event.getPlayer().setSprinting(false);
				if (applySlowness) {
					event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15, 2, false, false));
				}
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
