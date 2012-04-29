package zerothindex.bukkit.molotovcocktail;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraft.server.EntityPotion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftThrownPotion;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class EventListener implements Listener {

	private MolotovPlugin plugin;
	private Set<ThrownPotion> potions;
	
	public EventListener(MolotovPlugin p) {
		plugin = p;
		potions = new HashSet<ThrownPotion>();
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		// right click with fire charge? THROW A COCKTAIL HAHAHAHA
		if (e.getItem() != null && e.getItem().getType() == Material.FIREBALL
				&& (e.getAction().equals(Action.RIGHT_CLICK_AIR)
						|| (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)))) {
			
			if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				Material mat = e.getClickedBlock().getType();
				if (mat == Material.CHEST || mat == Material.STONE_BUTTON || mat == Material.LEVER
						|| mat == Material.FURNACE || mat == Material.BURNING_FURNACE || mat == Material.TRAP_DOOR
						|| mat == Material.DISPENSER || mat == Material.WORKBENCH || mat == Material.BED_BLOCK
						|| mat == Material.WOODEN_DOOR|| mat == Material.DIODE_BLOCK_OFF 
						|| mat == Material.DIODE_BLOCK_ON || mat == Material.ENCHANTMENT_TABLE
						|| mat == Material.BREWING_STAND || mat == Material.ENDER_PORTAL_FRAME) {
					return;
				}
			}
			// cancel event and remove 1 firecharge
			e.setCancelled(true);
			ItemStack chargeStack = e.getItem();
			if (chargeStack.getAmount() <= 1) {
				//Can't set amount to 0 for some reason...
				e.getPlayer().setItemInHand(null);
			} else {
				//Deduct 1 charge from hand
				chargeStack.setAmount(chargeStack.getAmount() -1);
			}
			
			Location loc = e.getPlayer().getEyeLocation();
			int potionValue = 3; //fire resistance
			// EntityPotion(World, x,y,z, potionValue)
			EntityPotion potion = new EntityPotion(((CraftWorld)e.getPlayer().getWorld()).getHandle(),
					loc.getX(), loc.getY(), loc.getZ(),
					potionValue);
			((CraftWorld)e.getPlayer().getWorld()).getHandle().addEntity(potion);
			CraftThrownPotion thrownPotion = new CraftThrownPotion((CraftServer) Bukkit.getServer(), potion);
			potions.add(thrownPotion); // keep track of what is a molotov cocktail
			thrownPotion.setShooter(e.getPlayer());
			thrownPotion.setVelocity(e.getPlayer().getLocation().getDirection());//.multiply(0.6));
			thrownPotion.setFireTicks(200);
		}
	}
	
	// called only when no entities were hit by the potion!
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.getEntity() instanceof ThrownPotion) {
			splash((ThrownPotion)e.getEntity());
		}
			
		
	}
	
	// called only when entities were hit by the potion!
	@EventHandler
	public void onPotionSplash(PotionSplashEvent e) {
		if (splash(e.getPotion())) {
			// do bonus damage to players in range
			e.getPotion().getEffects().clear();
			Iterator<LivingEntity> iter = e.getAffectedEntities().iterator();
			LivingEntity entity;
			while (iter.hasNext()) {
				entity = iter.next();
				e.setIntensity(entity, 0); // dont apply thrownpotion effects
				entity.setFireTicks(20* 10); // extra fire, 10 seconds
				if (plugin.critDamage && !entity.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
					entity.addPotionEffect(new PotionEffect(PotionEffectType.HARM, 1, 0));
				}
				
			}
		}
	}
	
	/**
	 * Calculate and place fire if the potion is actually a Molotov Cocktail!
	 * @param potion 
	 * @return true iff potion was a cocktail
	 */
	private boolean splash(ThrownPotion potion) {
		if (potions.contains(potion)) {
			long startTime = System.currentTimeMillis();
			
			potions.remove(potion);
			// find block where potion exploded
			Location centerLoc = potion.getLocation();
			int i = 4;
			while (i > 0 && !isIgnitable(centerLoc.getBlock().getType())) {
				centerLoc.subtract(potion.getVelocity());
				--i;
			}
			Vector center = centerLoc.toVector();
			center.setX(center.getBlockX()+0.5);
			center.setY(center.getBlockY()+0.5);
			center.setZ(center.getBlockZ()+0.5);
			// vector calculations
			// calculate rays from center
			// for each ray from center:
			//     while ray has next block within radius
			//       block = next block on ray
			//       if block at vector is ignitable
			//         ignite it
			//       else
			//         break
			
			// calculate a bunch of rays and iterate along them
			// Uses 'Golden Section Spiral' method from:
			//   http://www.xsi-blog.com/archives/115
			double inc = Math.PI * (3.0 - Math.sqrt(5));
			double off = 2.0/plugin.rays;
			double y,r,phi;
			BlockIterator blockIter;
			Block next;
			for (int k = 0; k < plugin.rays; ++k) {
				y = k * off - 1 + (off/2.0);
				r = Math.sqrt(1- y*y);
				phi = k*inc;
				Vector dirVector = new Vector(Math.cos(phi)*r, y, Math.sin(phi)*r);
				blockIter = new BlockIterator(potion.getWorld(),
												center,
												dirVector,
												0,
												plugin.radius);
				while (blockIter.hasNext()) {
					next = blockIter.next();
					if (isIgnitable(next.getType())) {
						ignite(next, (Player)potion.getShooter());
					} else {
						break;
					}
				}
			}	
			if (plugin.debug) {
				System.out.println("Completed splash. Elapsed: "+(System.currentTimeMillis()-startTime));
			}
			return true;			
		} else {
			return false;
		}
	}
	
	private boolean isIgnitable(Material mat) {
		return (mat == Material.AIR || mat == Material.FIRE  || mat == Material.LONG_GRASS
				|| mat == Material.RED_ROSE || mat == Material.YELLOW_FLOWER);
	}
	
	/**
	 * Will ignite ANY block and replace i with fire
	 * @param b
	 */
	private void ignite(Block block, Player player) {
		BlockIgniteEvent igniteEvent = new BlockIgniteEvent(block, 
				BlockIgniteEvent.IgniteCause.SPREAD, player);
		Bukkit.getPluginManager().callEvent(igniteEvent);
		if (!igniteEvent.isCancelled()) {
			block.setType(Material.FIRE);
		}
	}

}