package ru.example.customattributes;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Способности, срабатывающие при ударе игрока (предмет в левой руке). */
public class AbilityListener implements Listener {

    private final CustomAttributesPlugin plugin;
    private final AbilityManager abilities;
    private final NamespacedKey capKey;
    private final Map<String, Integer> counters = new HashMap<>();
    private final Map<UUID, Long> elytraBlocked = new HashMap<>();

    public AbilityListener(CustomAttributesPlugin plugin, AbilityManager abilities) {
        this.plugin = plugin;
        this.abilities = abilities;
        // тот же ключ, что и у атрибута «Золотое сердце» — очистка общая
        this.capKey = new NamespacedKey(plugin, "golden_heart_cap");
    }

    private double d(String key, double def) {
        return plugin.getConfig().getDouble("ability-settings." + key, def);
    }

    private int ticks(String key, double defSeconds) {
        return (int) Math.round(d(key, defSeconds) * 20);
    }

    private boolean roll(double percent) {
        return percent > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }

    /** Увеличивает счётчик ударов и сообщает, пришёлся ли удар на каждый N-й. */
    private boolean every(Player p, Ability a, String key, int def) {
        int n = Math.max(1, (int) d(key, def));
        String k = p.getUniqueId() + ":" + a.id;
        int count = counters.merge(k, 1, Integer::sum);
        return count % n == 0;
    }

    public boolean isElytraBlocked(Player p) {
        Long until = elytraBlocked.get(p.getUniqueId());
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            elytraBlocked.remove(p.getUniqueId());
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player att)) return;
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        if (target == att) return;

        Set<Ability> set = abilities.getAbilities(att.getInventory().getItemInOffHand());
        if (set.isEmpty()) return;

        double add = 0;
        double mult = 1;
        boolean inWater = att.isInWater();

        // 1) каждый 10-й удар — двойной урон
        if (set.contains(Ability.TENTH_HIT) && every(att, Ability.TENTH_HIT, "tenth-hit-every", 10)) {
            mult *= 2;
        }

        // 2) удар Хранителя
        if (set.contains(Ability.WARDEN_STRIKE) && roll(d("warden-chance", 3))) {
            add += d("warden-damage", 20);
            target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1);
            target.getWorld().playSound(target.getLocation(), "entity.warden.sonic_boom", 1f, 1f);
        }

        // 3) скорость при бое
        if (set.contains(Ability.COMBAT_SPEED)) {
            att.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,
                    ticks("combat-speed-seconds", 2), 0, true, false, true));
        }

        // 9) Холодная стойкость: пока не горите, шанс при ударе получить Сопротивление
        if (set.contains(Ability.CALM_RESIST) && att.getFireTicks() <= 0
                && roll(d("calm-resist-chance", 5))) {
            att.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                    ticks("calm-resist-seconds", 3), (int) d("calm-resist-amplifier", 1), true, false, true));
        }

        // 21) Вампирский укус
        if (set.contains(Ability.VAMPIRE_BITE) && roll(d("vampire-chance", 10))) {
            AttributeInstance mh = att.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = mh == null ? 20.0 : mh.getValue();
            att.setHealth(Math.min(maxHp, att.getHealth() + d("vampire-heal", 2)));
        }

        // 22) Ледяное касание
        if (set.contains(Ability.FROST_STRIKE) && roll(d("frost-strike-chance", 8))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    ticks("frost-strike-seconds", 3), (int) d("frost-strike-amplifier", 1)));
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                    20, 0.3, 0.5, 0.3, 0.02);
        }

        // 23) Поджигающий удар
        if (set.contains(Ability.IGNITE_STRIKE) && roll(d("ignite-chance", 8))) {
            target.setFireTicks(Math.max(target.getFireTicks(), ticks("ignite-seconds", 4)));
        }

        // 24) Гнев небес (молния без урона от самой молнии + доп. урон)
        if (set.contains(Ability.SKY_WRATH) && roll(d("sky-wrath-chance", 2))) {
            add += d("sky-wrath-damage", 6);
            target.getWorld().strikeLightningEffect(target.getLocation());
        }

        // 4) +2 урона в воде
        if (set.contains(Ability.WATER_DAMAGE) && inWater) {
            add += d("water-damage", 2);
        }

        // 6) шанс тройного урона в воде
        if (set.contains(Ability.WATER_TRIPLE) && inWater && roll(d("water-triple-chance", 3))) {
            mult *= 3;
        }

        // 19) палач
        if (set.contains(Ability.EXECUTIONER)
                && target.getHealth() < d("executioner-hearts", 5) * 2.0) {
            add += d("executioner-damage", 2);
        }

        // 10) золотые сердца при ударе
        if (set.contains(Ability.GOLDEN_STRIKE) && roll(d("golden-strike-chance", 3))) {
            giveAbsorption(att, d("golden-strike-hearts", 4) * 2.0);
        }

        // 12) слабость
        if (set.contains(Ability.WEAKENING_STRIKE) && roll(d("weakening-chance", 5))) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,
                    ticks("weakening-seconds", 2), 0));
        }

        // 14) каждый 15-й удар — левитация
        if (set.contains(Ability.LIFT_STRIKE) && every(att, Ability.LIFT_STRIKE, "lift-every", 15)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,
                    ticks("lift-seconds", 2), 0));
        }

        // 18) каждый 20-й удар — тошнота и слабость
        if (set.contains(Ability.NAUSEA_STRIKE) && every(att, Ability.NAUSEA_STRIKE, "nausea-every", 20)) {
            int t = ticks("nausea-seconds", 3);
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, t, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, t, 0));
        }

        // 15) блокировка элитр
        if (set.contains(Ability.ELYTRA_JAM) && target instanceof Player tp
                && roll(d("elytra-jam-chance", 5))) {
            double sec = d("elytra-jam-seconds", 4);
            elytraBlocked.put(tp.getUniqueId(), System.currentTimeMillis() + (long) (sec * 1000));
            if (tp.isGliding()) tp.setGliding(false);
            String msg = plugin.getConfig().getString("messages.elytra-blocked",
                    "&cВаши элитры заблокированы на %seconds% сек!");
            tp.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(msg.replace("%seconds%", String.valueOf((int) sec))));
        }

        if (mult != 1 || add != 0) {
            e.setDamage(e.getDamage() * mult + add);
        }

        // 17) поломка брони
        if (set.contains(Ability.ARMOR_BREAKER) && roll(d("armor-break-chance", 10))) {
            int vanilla = Math.max(1, (int) (e.getDamage() / 4.0));
            int extra = Math.max(1, (int) Math.round(vanilla * (d("armor-break-multiplier", 2.0) - 1.0)));
            wearArmor(target, extra);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (e.isGliding() && isElytraBlocked(p)) e.setCancelled(true);
    }

    // 25) Мягкое приземление: меньше урона от падения
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player p)) return;
        Set<Ability> set = abilities.getAbilities(p.getInventory().getItemInOffHand());
        if (!set.contains(Ability.SOFT_LANDING)) return;
        double red = Math.min(100.0, d("soft-landing-reduction", 50));
        e.setDamage(e.getDamage() * (1.0 - red / 100.0));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        elytraBlocked.remove(id);
        counters.keySet().removeIf(k -> k.startsWith(id.toString()));
    }

    /* ---------- вспомогательное ---------- */

    private void wearArmor(LivingEntity target, int extra) {
        EntityEquipment eq = target.getEquipment();
        if (eq == null) return;
        ItemStack[] arr = eq.getArmorContents();
        boolean changed = false;
        for (int i = 0; i < arr.length; i++) {
            ItemStack it = arr[i];
            if (it == null || it.getType().isAir()) continue;
            int max = it.getType().getMaxDurability();
            if (max <= 0) continue;
            ItemMeta meta = it.getItemMeta();
            if (meta == null || meta.isUnbreakable()) continue;
            if (!(meta instanceof Damageable)) continue;
            Damageable dm = (Damageable) meta;
            int nd = dm.getDamage() + extra;
            if (nd >= max) {
                arr[i] = null;
                target.getWorld().playSound(target.getLocation(), "entity.item.break", 1f, 1f);
            } else {
                dm.setDamage(nd);
                it.setItemMeta(meta);
            }
            changed = true;
        }
        if (changed) eq.setArmorContents(arr);
    }

    /** Золотые сердца: поднимаем лимит MAX_ABSORPTION временным модификатором. */
    private void giveAbsorption(Player p, double hp) {
        double max = plugin.getConfig().getDouble("golden-heart.max-absorption", 20.0);
        AttributeInstance inst = p.getAttribute(Attribute.MAX_ABSORPTION);
        if (inst == null) return;
        if (inst.getModifier(capKey) == null) {
            inst.addTransientModifier(new AttributeModifier(capKey, max, AttributeModifier.Operation.ADD_NUMBER));
        }
        p.setAbsorptionAmount(Math.min(p.getAbsorptionAmount() + hp, max));
    }
}
