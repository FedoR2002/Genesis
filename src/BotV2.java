public class BotV2 implements Cloneable{
    // describing Bot class

    // properties:
    // populationid
    private int populationid;
    // birth (tick)
    private long birthtick;
    // HP
    private int hp;

    public int getHP() {
        return hp;
    }

    public void setHP(int hp) {
        this.hp = hp;
    }

    // color
    private int b_red;
    private int b_blue;
    private int b_green;
    // genom
    private byte[] bot_genom = new byte[Consts.MIND_SIZE];
    // direction
    private int direction = 0;
    // position
    private MapPosition position;
    private MapPosition viewPoint(){
        MapPosition newxy = new MapPosition(position);
        switch (direction) {
            case 0:
                newxy.x--;
                newxy.y--;
                break;
            case 1:
                newxy.y--;
                break;
            case 2:
                newxy.x++;
                newxy.y--;
                break;
            case 3:
                newxy.x++;
                break;
            case 4:
                newxy.x++;
                newxy.y++;
                break;
            case 5:
                newxy.y++;
                break;
            case 6:
                newxy.x--;
                newxy.y++;
                break;
            case 7:
                newxy.x--;
                break;
        }
        return newxy;
    }

    // actions:
    // ending:
    // roll on genom
    // eat (sun)
    private int botEatSun() {
        PlaceProp posProp = WorldV2.getInstance().getPlaceProp(position);
        hp += (int) (posProp.getSunLevel() * Consts.SUN2HP_COEFF);
        hp = Math.min(hp, Consts.MAX_BOT_HP);
        goGreen();
        return 2;
    }

    // eat (minerals)
    private int botEatMinerals() {
        PlaceProp posProp = WorldV2.getInstance().getPlaceProp(position);
        hp += (int) (posProp.getMineralLevel() * Consts.MIN2HP_COEFF);
        hp = Math.min(hp, Consts.MAX_BOT_HP);
        goBlue();
        return 2;
    }

    // eat (organics)
    private int botEatOrganics() {
        PlaceProp posProp = WorldV2.getInstance().getPlaceProp(position);
        hp += (int) (posProp.getOrganicLevel() * Consts.ORG2HP_COEFF);
        hp = Math.min(hp, Consts.MAX_BOT_HP);
        goBlack();
        return 2;
    }

    // attack
    private int botEatOther() {
        BotV2 otherBot = WorldV2.getInstance().getBotAtPos(viewPoint());
        // battle
        if (otherBot.getHP() == hp) {
            //both divide HP;
            WorldV2.getInstance().getBotAtPos(viewPoint()).setHP(otherBot.getHP() / 2);
            hp /= 2;
        } else {
            if (otherBot.getHP() < hp) {
                // kill other
                int org_level = otherBot.getHP();
                WorldV2.getInstance().getBotAtPos(viewPoint()).die();
                WorldV2.getInstance().eliminateBot(viewPoint());
                moveTo(viewPoint());
                botEatOrganics();
                goRed();
            } else {

                die();
                WorldV2.getInstance().eliminateBot(position);
            }
        }
        return 2;

    }
    // move 1 step
    private int moveTo(MapPosition targetPos){
        WorldV2.getInstance().moveBot(position, targetPos);
        position = targetPos;
        return 2;
    }
    // divide
    private int divide(){
        for (int i=0;i<8;i++){
            if (!WorldV2.getInstance().checkBotAtPos(viewPoint())){
                WorldV2.getInstance().incMutation_count();
                if (WorldV2.getInstance().getMutation_count()%100 == 0) {
                    byte[] new_genom = mutate_genom(3);
                    WorldV2.getInstance().createNewBot(this, hp/2, viewPoint(), new_genom);
                }
                else
                    WorldV2.getInstance().createNewBot(this, hp/2, viewPoint());

                break;
            }
            rotate();
        }
        hp/=2;
        return 0;
    }

    private byte[] mutate_genom(int c) {
        byte [] new_genom = bot_genom.clone();
        for (int i=0;i<c;i++){
            int pos = (int)(Math.random() * Consts.MIND_SIZE);
            int shift = (int)(Math.random() * Consts.MIND_SIZE);
            new_genom[pos] = (byte)((new_genom[pos]+shift)%256);
        }
        return new_genom;
    }

    // die
    public void die(){
        PlaceProp prop = WorldV2.getInstance().getPlaceProp(viewPoint());
        prop.setOrganicLevel(prop.getOrganicLevel() + hp);
        WorldV2.getInstance().setPlaceProp(viewPoint(), prop);
    }
    // non-ending:
    // rotate
    private void rotate(){
        direction = (direction+1)%8;
    }
    // check
    // colors
    private void goGreen(){
        b_green = Math.min(b_green+32, 255);
        b_blue = Math.max(b_blue-32, 0);
        b_red = Math.max(b_red-32,0);
    }
    private void goBlue(){
        b_green = Math.max(b_green-32, 0);
        b_blue = Math.min(b_blue+32, 255);
        b_red = Math.max(b_red-32,0);
    }
    private void goRed(){
        b_green = Math.max(b_green-32, 0);
        b_blue = Math.max(b_blue-32, 0);
        b_red = Math.min(b_red+32,255);
    }
    private void goBlack(){
        b_green = Math.max(b_green-32, 0);
        b_blue = Math.max(b_blue-32, 0);
        b_red = Math.max(b_red-32,0);
    }
}
