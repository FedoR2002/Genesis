import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Strain {
    // strain lifetime
    private final int birthIteration;
    private int lastIteration;
    private int currentPopulation;
    private int maxPopulation;
    private boolean aliveStrain;
    private boolean stableStrain;

    private double averageLifeTime;

    private final byte [] genom;
    private final int strainColor;

    private final List<Bot> currentBots;

    private final List<Integer> childStrainCreations = new ArrayList<>();
    private final List<Integer> botLifeTimes = new ArrayList<>();

    public Strain(int birthIteration, Bot firstBot) {
        this.birthIteration = birthIteration;
        lastIteration = -1;
        currentPopulation = 1;
        maxPopulation = 1;
        aliveStrain = true;
        stableStrain = false;
        averageLifeTime = 0;
        genom = firstBot.mind.clone();
        strainColor = firstBot.c_family;
        currentBots = new ArrayList<>();
        currentBots.add(firstBot);
    }

    public boolean isStableStrain() {return stableStrain;}
    public boolean isAliveStrain() {return aliveStrain;}

    public int getMaxPopulation() {return maxPopulation;}

    public double getAverageLifeTime() {
        return averageLifeTime;
    }

    public int getBirthIteration() {
        return birthIteration;
    }
    public int getLastIteration() {
        return lastIteration;
    }

    public void addBot(Bot bot){
        currentBots.add(bot);
        currentPopulation++;
        maxPopulation = Math.max(maxPopulation, currentPopulation);
        if (currentPopulation>=Consts.STRAIN_STABLE_THRESHOLD)
            stableStrain = true;
        bot.setStrain(this);
    }

    public void removeBot(Bot bot) {
        if (currentBots.contains(bot))
            currentBots.remove(bot);
        currentPopulation--;
//        if (currentPopulation < Consts.STRAIN_STABLE_THRESHOLD)
//            stableStrain = false;
        if (currentPopulation == 0)
            aliveStrain = false;
        botLifeTimes.add(bot.age);
        Integer s = 0;
        for (Integer el : botLifeTimes) s += el;
        averageLifeTime = 1.0f * s / botLifeTimes.size();

        if (!aliveStrain) {
            botLifeTimes.clear();
            lastIteration = World.simulation.generation;
        }
        bot.setStrain(null);
    }

    public void runAllBots() {
        for (Bot currBot:currentBots) {
            currBot.step();
        }
    }
}
