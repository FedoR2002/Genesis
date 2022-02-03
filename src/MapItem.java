public class MapItem {
    // level
    public int level;
    // resources
    public int sunPower; // unlimited, уровень солнечной энергии, поглощается "растениями"
    public int organicLevel; // уровень органики на клетке. Может быть поглощена "падальщиками" 1oL->4HP, распадается со временем
                                // 4oL->1mL за ход.
    public int mineralLevel; // unlimited при level<sealevel, уровень минералов, поглощается "растениями"
    public Bot bot; // живое существо, в случае смерти от нехватки места оставляет после себя органику 1HP->1oL,
                    // при естественной смерти или неудачной атаке оставляет 100oL
}
