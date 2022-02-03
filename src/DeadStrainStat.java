public class DeadStrainStat {
    public int maxPopulation;
    public double averageAge;
    public int startStep;
    public int deadStep;

    public DeadStrainStat(Strain strain){
        maxPopulation = strain.getMaxPopulation();
        averageAge = strain.getAverageLifeTime();
        startStep = strain.getBirthIteration();
        deadStep = strain.getLastIteration();
    }
}
