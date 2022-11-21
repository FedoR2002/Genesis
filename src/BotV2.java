import java.util.Arrays;

public class BotV2 implements Cloneable{
    // describing Bot class

    // properties:
    // populationid
    private int populationid;
    // birth (tick)
    private long birthtick;
    private long age=0;
    // HP
    private int hp;
    private int adr=0;

    public BotV2() {
        Arrays.fill(bot_genom, (byte) 32);
    }

    public int getHP() {
        return hp;
    }

    public void setHP(int hp) {
        this.hp = hp;
    }

    // color
    private int b_red = 170;
    private int b_blue = 170;
    private int b_green =170;
    // genom
    private byte[] bot_genom = new byte[Consts.MIND_SIZE];
    // direction
    private int direction = 5;
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
    // see bot?
    private int bot_CheckAtView() {
        if (!WorldV2.getInstance().checkBotAtPos(viewPoint()))
            return 1;
        else
            return 2;
    }

    // divide
    private int bot_Divide(){
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
        PlaceProp prop = WorldV2.getInstance().getPlaceProp(position);
        prop.setOrganicLevel(prop.getOrganicLevel() + hp);
        WorldV2.getInstance().setPlaceProp(position, prop);
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

    public int getColor(){
        return (255 << 24) | (b_red << 16) | (b_green << 8) | b_blue;
    }

    public MapPosition getPosition() {
        return position;
    }

    public void setPosition(MapPosition viewPoint) {
        position = viewPoint;
    }

    public void setBot_genom(byte[] new_genom) {
        bot_genom = new_genom.clone();
//        System.arraycopy(new_genom, 0, bot_genom, 0, Consts.MIND_SIZE);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    // ====================================================================
    // =========== главная функция жизнедеятельности бота  ================
    // =========== в ней выполняется код его мозга-генома  ================
    void step() {
        int breakflag;
        int command;
        for (int cyc = 0; cyc < 15; cyc++) {
            command = bot_genom[adr];        // текущая команда
            breakflag = 0;
            switch (command) {

//*******************************************************************
//................      мутировать   ................................
                case 0:
                    setBot_genom(mutate_genom(2));
                    botIncAdr(1);   // смещаем указатель текущей команды на 1
                    breakflag = 1;     // выходим, так как команда завершающая
                    break;

//*******************************************************************
//............... размножение делением ..............................
                case 16:
                        bot_Divide();
                        botIncAdr(1);
                        breakflag = 1;
                    break;


//*******************************************************************
//...............  повернуть с параметром   .........................
                case 23:
                    rotate();
                    botIncAdr(2);
                    break;
//*******************************************************************
//...............  шаг с параметром  ................................
//                case 26:
//                    botJumpAdr( botMove()); // смещаем УТК на значение клетки (botMove(): 2-пусто  3-стена  4-органика 5-бот 6-родня)
//                    breakflag = 1;
//                    break;


//*******************************************************************
//...............  фотосинтез .......................................
                case 32:
                    int decHP=botEatSun();
                    botIncAdr(1);
                    hp -= decHP;
                    breakflag = 1;
                    break;
//*******************************************************************
//............... хемосинтез (энерия из минералов) ..................
//                case 33:
//                    botEatMinerals();
//                    botIncAdr(1);
//                    breakflag = 1;
//                    break;
//*******************************************************************
//............... хемосинтез (энерия из органики) ..................
//                case 34:
//                    botEatOrganics();
//                    botIncAdr(1);
//                    breakflag = 1;
//                    break;
//*******************************************************************
//............... хемосинтез (энерия из минералов) ..................
//                case 35:
//                    botMineral2Health();
//                    botIncAdr(1);
//                    breakflag = 1;
//                    break;
//************************************************************************
//..............   съесть в относительном напралении       ...............
                case 36:
                    botJumpAdr(botEatOther()); // меняем адрес текущей команды
                    // стена - 2 пусто - 3 органика - 4 живой - 5
                    breakflag = 1;
                    break;


//************************************************************************
//.............   отдать безвозмездно в относительном напралении  ........
//                case 36:
//                case 37:    // увеличил шансы появления этой команды
//                    botJumpAdr(botGive()); // меняем адрес текущей команды
//                    // стена - 2 пусто - 3 органика - 4 удачно - 5
//                    break;
//************************************************************************
//.............   распределить энергию в относительном напралении  .......
//                case 38:
//                case 39:    // увеличил шансы появления этой команды
//                    botJumpAdr(botCare()); // меняем адрес текущей команды
//                    // стена - 2 пусто - 3 органика - 4 удачно - 5
//                    break;


//************************************************************************
//.............   посмотреть с параметром ................................
                case 40:
                    botJumpAdr(bot_CheckAtView()); // меняем адрес текущей команды
                    // пусто - 2 стена - 3 органик - 4 бот -5 родня -  6
                    break;


//***********************************************************************
//...................  проверка уровня рельефа  .........................
//                case 41:    // checkLevel() берет параметр из генома, возвращает 2, если рельеф выше, иначе - 3
//                    botJumpAdr(checkLevel());
//                    break;
//***********************************************************************
//...................  проверка здоровья  ...............................
//                case 42:    // checkHealth() берет параметр из генома, возвращает 2, если здоровья больше, иначе - 3
//                    botJumpAdr(checkHealth());
//                    break;
//***********************************************************************
//...................  проверка  минералов ..............................
//                case 43:    // checkMineral() берет параметр из генома, возвращает 2, если минералов больше, иначе - 3
//                    botJumpAdr(checkMineral());
//                    break;


//*************************************************************
//...............  окружен ли бот?   ..........................
//                case 46:   // isFullAroud() возвращает  1, если бот окружен и 2, если нет
//                    botJumpAdr(isFullAround());
//                    break;
//*************************************************************
//.............. приход энергии есть? .........................
//                case 47:  // isHealthGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
//                    botJumpAdr(isHealthGrow());
//                    break;
//*************************************************************
//............... минералы прибавляются? ......................
//                case 48:   // isMineralGrow() возвращает 1, если энегрия у бота прибавляется, иначе - 2
//                    botJumpAdr(isMineralGrow());
//                    break;
//

//********************************************************************
//................   генная атака  ...................................
//                case 52:  // бот атакует геном соседа, на которого он повернут
//                    botGenAttack(); // случайным образом меняет один байт
//                    botIncAdr(1);
//                    breakflag = 1;
//                    break;

//=======================================================================
//................    если ни с одной команд не совпало .................
//................    значит безусловный переход        .................
//.....   прибавляем к указателю текущей команды значение команды   .....
                default:
                    botIncAdr(command);
                    break;
            }
            if (breakflag == 1) break;
        }

//###########################################################################
//.......  выход из функции и передача управления следующему боту   ........
//.......  но перед выходом нужно проверить                         ........
//.......  количество накопленой энергии, возможно                  ........
//.......  пришло время подохнуть или породить потомка              ........

            //... проверим уровень энергии у бота, возможно пришла пора помереть или родить
            if (hp > 990) {                     // если энергии больше 999, то плодим нового бота
                bot_Divide();
            }

//            hp -= 3;                            // каждый ход отнимает 3 единицы энегрии
            age++;                                  // увеличиваем возраст
            if (hp <= 5) {                      // если энергии стало меньше 5
                die();
                WorldV2.getInstance().eliminateBot(position);
                return;                             // и передаем управление к следующему боту
            }
            hp -= 3;
    }

    // -- увеличение адреса команды   --------------
    private void botIncAdr(int a) {
        adr = (adr + a) % Consts.MIND_SIZE;
    }

    //---- косвенное увеличение адреса команды   --------------
    private void botJumpAdr(int a) {
        int bias = bot_genom[(adr + a) % Consts.MIND_SIZE];
        botIncAdr(bias);
    }

}
