public class MapPosition {
    public int x;
    public int y;

    MapPosition() {
        x=0;y=0;
    }

    MapPosition(int x, int y){
        this.x = x;
        this.y = y;
    }

    MapPosition(MapPosition pos){
        this.x = pos.x;
        this.y = pos.y;
    }
}
