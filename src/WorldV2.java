import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldV2 {

    private static WorldV2 instance = null;
    private int width, height;
    private float[][] mapHeightLayer;
    private PlaceProp[][] mapObjectsLayer;
    private UUID[][] mapBotsLayer;
    private Map<UUID, BotV2> allBots = new HashMap<UUID, BotV2>();

    private int mutation_count=0;
    public int getMutation_count(){return mutation_count;}
    public void incMutation_count(){mutation_count++;}

    private WorldV2() {
    }

    public static WorldV2 getInstance() {
        if (instance == null)
            instance = new WorldV2();
        return instance;
    }

    public PlaceProp getPlaceProp(MapPosition pos) {
        int xx = pos.x % width;
        int yy = pos.y % height;
        return mapObjectsLayer[xx][yy];
    }

    public void setPlaceProp(MapPosition pos, PlaceProp prop) {
        int xx = pos.x % width;
        int yy = pos.y % height;
        mapObjectsLayer[xx][yy].setSunLevel(prop.getSunLevel());
        mapObjectsLayer[xx][yy].setMineralLevel(prop.getMineralLevel());
        mapObjectsLayer[xx][yy].setOrganicLevel(prop.getOrganicLevel());
        mapObjectsLayer[xx][yy].setRadiationLevel(prop.getRadiationLevel());
    }


    public void eliminateBot(MapPosition pos) {
        int xx = pos.x % width;
        int yy = pos.y % height;

        UUID botUID = mapBotsLayer[xx][yy];
        mapBotsLayer[xx][yy] = null;
        // remove bot from arrays
        // common array
        allBots.remove(botUID);
    }

    public BotV2 getBotAtPos(MapPosition pos) {
        int xx = pos.x % width;
        int yy = pos.y % height;
        return allBots.get(mapBotsLayer[xx][yy]);
    }

    public MapPosition moveBot(MapPosition position, MapPosition targetPos) {
        int xx = targetPos.x % width;
        int yy = targetPos.y % height;
        mapBotsLayer[xx][yy] = mapBotsLayer[position.x][position.y];
        mapBotsLayer[position.x][position.y] = null;
        return new MapPosition(xx,yy);
    }

    public boolean checkBotAtPos(MapPosition viewPoint) {
        int xx = viewPoint.x % width;
        int yy = viewPoint.y % height;

        return mapBotsLayer[xx][yy]!=null;
    }

    public void createNewBot(BotV2 botV2, int i, MapPosition viewPoint) {

    }
}
