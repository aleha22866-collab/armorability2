package ru.example.customattributes;

public enum Ability {
    TENTH_HIT("tenth_hit", "&6Десятый удар", "&7Каждый десятый удар", "&7наносит двойной урон"),
    WARDEN_STRIKE("warden_strike", "&3Удар Хранителя", "&7Шанс 3% ударить звуковой волной", "&7и нанести 20 доп. урона"),
    COMBAT_SPEED("combat_speed", "&bБоевой темп", "&7Скорость I, пока вы наносите", "&7удары каждые 2 секунды"),
    WATER_DAMAGE("water_damage", "&9Сила прилива", "&7+2 к урону,", "&7пока вы находитесь в воде"),
    WATER_ARMOR("water_armor", "&9Водяной панцирь", "&7+1 к броне,", "&7пока вы находитесь в воде"),
    WATER_TRIPLE("water_triple", "&1Гнев пучины", "&7Шанс 3% нанести тройной урон,", "&7пока вы находитесь в воде"),
    CALM_REGEN("calm_regen", "&aХолодное исцеление", "&7Регенерация I,", "&7пока вы не горите"),
    CALM_STRENGTH("calm_strength", "&cХолодная ярость", "&7Сила I,", "&7пока вы не горите"),
    CALM_RESIST("calm_resist", "&8Холодная стойкость", "&7Пока вы не горите, шанс 5% при ударе", "&7получить Сопротивление II на 3 секунды"),
    GOLDEN_STRIKE("golden_strike", "&eЗолотой удар", "&7Шанс 3% при ударе получить", "&7+4 золотых сердца"),
    ADVERSITY("adversity", "&dСтойкость в беде", "&7При любом негативном эффекте даёт", "&7Сопротивление I и Регенерацию I"),
    WEAKENING_STRIKE("weakening_strike", "&7Ослабляющий удар", "&7Шанс 5% наложить Слабость I", "&7на 2 секунды на противника"),
    ELYTRA_REGEN("elytra_regen", "&fИсцеляющий полёт", "&7Регенерация II,", "&7пока вы летите на элитрах"),
    LIFT_STRIKE("lift_strike", "&5Подбрасывающий удар", "&7Каждый 15 удар накладывает", "&7Левитацию I на 2 секунды"),
    ELYTRA_JAM("elytra_jam", "&3Глушитель крыльев", "&7Шанс 5% заблокировать элитры", "&7противника на 4 секунды"),
    HAND_POWER("hand_power", "&6Благословение руки", "&7Сила I и Скорость I,", "&7пока предмет в левой руке"),
    ARMOR_BREAKER("armor_breaker", "&4Крушитель брони", "&7Шанс 10% сломать броню", "&7противника в 2 раза сильнее"),
    NAUSEA_STRIKE("nausea_strike", "&2Дурманящий удар", "&7Каждый 20 удар накладывает", "&7Тошноту I и Слабость I на 3 секунды"),
    EXECUTIONER("executioner", "&4Палач", "&7+2 к урону, когда у противника", "&7меньше 5 сердец здоровья"),
    LAST_STAND("last_stand", "&cПоследний рубеж", "&7+2 к броне, когда у вас", "&7меньше 5 сердец здоровья"),
    VAMPIRE_BITE("vampire_bite", "&4Вампирский укус", "&7Шанс 10% при ударе", "&7восстановить себе 1 сердце"),
    FROST_STRIKE("frost_strike", "&bЛедяное касание", "&7Шанс 8% при ударе наложить", "&7Замедление II на 3 секунды"),
    IGNITE_STRIKE("ignite_strike", "&6Поджигающий удар", "&7Шанс 8% при ударе", "&7поджечь противника на 4 секунды"),
    SKY_WRATH("sky_wrath", "&eГнев небес", "&7Шанс 2% при ударе вызвать молнию", "&7и нанести 6 доп. урона"),
    SOFT_LANDING("soft_landing", "&aМягкое приземление", "&7-50% к урону от падения,", "&7пока предмет в левой руке"),
    GILLS("gills", "&3Жабры", "&7Подводное дыхание, а в воде", "&7ещё и Грация дельфина"),
    SECOND_WIND("second_wind", "&dВторое дыхание", "&7При здоровье меньше 3 сердец: Регенерация III", "&7и +2 золотых сердца (откат 60 сек)");

    public final String id;
    public final String defaultName;
    public final String lore1;
    public final String lore2;

    Ability(String id, String defaultName, String lore1, String lore2) {
        this.id = id;
        this.defaultName = defaultName;
        this.lore1 = lore1;
        this.lore2 = lore2;
    }

    public static Ability fromId(String s) {
        for (Ability a : values()) {
            if (a.id.equalsIgnoreCase(s)) return a;
        }
        return null;
    }
}
