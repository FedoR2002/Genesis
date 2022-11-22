import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StrainV2 {
    private boolean dead;
    private UUID populationId;
    private byte[] genom;
    private List<BotV2> botList = new ArrayList<>();
    private List<Integer> deadBotAges = new ArrayList<>();
    private int averageAge;
    private int strainColor;

    public StrainV2(BotV2 firstBot){
        dead = true;
        populationId = UUID.randomUUID();
        botList.add(firstBot);
        genom = firstBot.get_genom();
        strainColor = firstBot.getStrain_color();
    }

    public void addBot(BotV2 bot){
        botList.add(bot);
        bot.setPopulationid(populationId);
        recalc();
    }

    public boolean removeBot(BotV2 bot){
        deadBotAges.add(bot.getAge());
        botList.remove(bot);
        dead = botList.isEmpty();
        recalc();
        return dead;
    }

    public boolean isDead(){
        return dead;
    }

    public void recalc(){
        long sum=0;
        for (Integer age:deadBotAges)
            sum+= age;
        for (BotV2 bot:botList)
            sum+= bot.getAge();
        averageAge = (int) (sum / (deadBotAges.size() + botList.size()));
    }

    public int getAverageAge() {
        return averageAge;
    }

    public int getTotalPopulation() {return deadBotAges.size() + botList.size();}
    public int getCurrPopulation() {return botList.size();}

    public UUID getPopulationId() {
        return populationId;
    }
}
