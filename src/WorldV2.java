import Utils.CommonUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class WorldV2 implements GuiCallback {

    private static WorldV2 instance = null;
    private int zoom;
    private final int worldScale;
    private final Gui gui;

    private int[] mapInGPU;    //Карта для GPU
    private Image mapbuffer = null;

    private int width, height;
    private int[][] mapHeightLayer;
    private PlaceProp[][] mapObjectsLayer;
    private UUID[][] mapBotsLayer;
    private ConcurrentMap<UUID, BotV2> allBots = new ConcurrentHashMap<>();
    private LinkedList<UUID> botsLinkedList = new LinkedList<>();

    private int mutation_count=0;
    private int drawstep=10;
    private int perlinValue=300;
    private int sealevel=145;
    private int population;
    private int organic;
    private int viewMode;
    private int generation;


    private Thread thread = null;
    private boolean started = true; // поток работает?

    public int getMutation_count(){return mutation_count;}
    public void incMutation_count(){mutation_count++;}

    private WorldV2() {
        zoom = 1;
        worldScale = 100;
        int perlinValue = 300;
        int sealevel = 145;
        int drawstep = 10;
        gui = new Gui(this);
        gui.init();
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
        // remove bot from arrays
        if (botsLinkedList.contains(botUID)){
            botsLinkedList.remove(botUID);
            // common array
            allBots.remove(botUID);
        }
        mapBotsLayer[xx][yy] = null;
    }

    public BotV2 getBotAtPos(MapPosition pos) {
        int xx = pos.x % width;
        int yy = pos.y % height;
        if (botsLinkedList.contains(mapBotsLayer[xx][yy]))
            return allBots.get(mapBotsLayer[xx][yy]);

        else
            return null;
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

        if (botsLinkedList.contains(mapBotsLayer[xx][yy]))
            return mapBotsLayer[xx][yy]!=null;
        else
            return false;
    }

    public BotV2 createNewBot(BotV2 botV2, int i, MapPosition viewPoint) {
        BotV2 newBot = null;
        try {
            newBot = (BotV2) botV2.clone();
            newBot.resetAge();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        newBot.setHP(i);
        newBot.setPosition(viewPoint);

        int xx = viewPoint.x % width;
        int yy = viewPoint.y % height;
            UUID nbuid = UUID.randomUUID();
        while (botsLinkedList.contains(nbuid))
            nbuid = UUID.randomUUID();
        botsLinkedList.add(nbuid);
        mapBotsLayer[xx][yy] = nbuid;
        allBots.put(nbuid, newBot);
        return newBot;
    }

    public void createNewBot(BotV2 botV2, int i, MapPosition viewPoint, byte[] new_genom) {
        BotV2 newBot = createNewBot(botV2, i, viewPoint);
        if (newBot == null) return;
        newBot.setBot_genom(new_genom);
    }

    private void clearMapItems(){
        for (UUID botuid:botsLinkedList) {
            eliminateBot(allBots.get(botuid).getPosition());
        }
    }

    @Override
    public void drawStepChanged(int value) {
        this.drawstep = value;
    }

    @Override
    public void mapGenerationStarted(int canvasWidth, int canvasHeight) {
        clearMapItems();
        width = (int) (1.0f * canvasWidth / zoom);    // Ширина доступной части экрана для рисования карты
        height = (int) (1.0f * canvasHeight / zoom);
        generateMap((int) (Math.random() * 10000));
        generateAdam();
        paintMapView();
        paint1();
    }

    @Override
    public void seaLevelChanged(int value) {
        sealevel = value;
        if (mapHeightLayer != null) {
            paintMapView();
            paint1();
        }
    }

    @Override
    public boolean startedOrStopped() {
        if (thread == null) {
            thread = new WorldV2.Worker(); // создаем новый поток
            thread.start();
            return true;
        } else {
            started = false;        //Выставляем влаг
            CommonUtils.joinSafe(thread);
            thread = null;
            return false;
        }
    }

    @Override
    public void viewModeChanged(int viewMode) {
        this.viewMode = viewMode;
    }

    @Override
    public void setWorldScale(int worldScale) {
        this.zoom = worldScale;
    }

    @Override
    public void setPerlin(int perlin) {
        this.perlinValue = perlin;
    }

    // генерируем карту
    public void generateMap(int seed) {
        int generation = 0;
        this.mapHeightLayer = new int[width][height];
        this.mapBotsLayer = new UUID[width][height];
        this.mapObjectsLayer = new PlaceProp[width][height];

        Perlin2D perlin = new Perlin2D(seed);
        int sunPower=0;
        int mineralLevel=0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mapObjectsLayer[x][y] = new PlaceProp();
                float f = (float) perlinValue;
                float value = perlin.getNoise(x / f, y / f, 8, 0.45f);        // вычисляем точку ландшафта
                int h = (int) (value * 255 + 128) & 255;
                mapHeightLayer[x][y] = h;
//                worldMap[x][y].level = map[x][y];
                // set solar power
                if ((h > sealevel) && (h <= sealevel + 100))
                    sunPower = (int) ((sealevel + 100 - h) * 0.2); // формула вычисления энергии
                if ((h > sealevel - 50) && (h <= sealevel))
                    sunPower = (int) ((h - sealevel + 50) * 0.1); // формула вычисления энергии
                mapObjectsLayer[x][y].setSunLevel(sunPower);
                // set mineral level
                if ((h > sealevel - 50) && (h <= sealevel))
                    mineralLevel = (int) ((sealevel - h) * 0.2); // формула вычисления минералов
                mapObjectsLayer[x][y].setMineralLevel(mineralLevel);

            }
        }
        mapInGPU = new int[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                mapInGPU[j * width + i] = mapHeightLayer[i][j];
            }
        }
        CommonConsts.regenerate();
        CommonConsts.clearFamilies();

    }

    // генерируем первого бота
    public void generateAdam() {

        BotV2 bot = new BotV2();

//        bot.c_family = bot.get_c_family();
        createNewBot(bot, 500, new MapPosition(width /2, height / 2));

//        CommonConsts.familiesCount.put(bot.get_c_family(), 1);
//        currentbot = bot;                       // устанавливаем текущим
//        createNewStrain(bot);
    }

    public void paintMapView() {
        int mapred;
        int mapgreen;
        int mapblue;
        mapbuffer = gui.canvas.createImage(width * zoom, height * zoom); // ширина - высота картинки
        Graphics g = mapbuffer.getGraphics();

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < rgb.length; i++) {
            if (mapInGPU[i] < sealevel) {                     // подводная часть
                mapred = 5;
                mapblue = 140 - (sealevel - mapInGPU[i]) * 3;
                mapgreen = 150 - (sealevel - mapInGPU[i]) * 10;
                if (mapgreen < 10) mapgreen = 10;
                if (mapblue < 20) mapblue = 20;
            } else {                                        // надводная часть
                mapred = (int) (150 + (mapInGPU[i] - sealevel) * 2.5);
                mapgreen = (int) (100 + (mapInGPU[i] - sealevel) * 2.6);
                mapblue = 50 + (mapInGPU[i] - sealevel) * 3;
                if (mapred > 255) mapred = 255;
                if (mapgreen > 255) mapgreen = 255;
                if (mapblue > 255) mapblue = 255;
            }
            rgb[i] = (mapred << 16) | (mapgreen << 8) | mapblue;
        }
        g.drawImage(image, 0, 0, null);
    }


    //    @Override
    public void paint1() {

        Image buf = gui.canvas.createImage(width * zoom, height * zoom); //Создаем временный буфер для рисования
        Graphics g = buf.getGraphics(); //подеменяем графику на временный буфер
        g.drawImage(mapbuffer, 0, 0, null);

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] rgb = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        population = 0;
        organic = 0;
        int mapred, mapgreen, mapblue;

        for (UUID botuid:botsLinkedList) {
            try {
                if (viewMode == Consts.VIEW_MODE_BASE) {
                    rgb[allBots.get(botuid).getPosition().y * width + allBots.get(botuid).getPosition().x] = allBots.get(botuid).getColor();
                } else if (viewMode == Consts.VIEW_MODE_HP) {
                    mapgreen = 255 - (int) (allBots.get(botuid).getHP() * 0.25);
                    if (mapgreen < 0) mapgreen = 0;
                    rgb[allBots.get(botuid).getPosition().y * width + allBots.get(botuid).getPosition().x] = (255 << 24) | (0 << 16) | (mapgreen << 8);
                }
//                    else if (viewMode == Consts.VIEW_MODE_MINERAL) {
//                        mapblue = 255 - (int) (currentbot.mineral * 0.5);
//                        if (mapblue < 0) mapblue = 0;
//                        rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (0) | (255 << 8) | mapblue;
//                    }
//                    else if (viewMode == VIEW_MODE_COMBINED) {
//                        mapgreen = (int) (currentbot.c_green * (1 - currentbot.health * 0.0005));
//                        if (mapgreen < 0) mapgreen = 0;
//                        mapblue = (int) (currentbot.c_blue * (0.8 - currentbot.mineral * 0.0005));
//                        rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (mapgreen << 8) | mapblue;
//                    }
                else if (viewMode == Consts.VIEW_MODE_AGE) {
                    mapred = 255 - (int) (allBots.get(botuid).getAge() / 4);
                    if (mapred < 0) mapred = 0;
                    rgb[allBots.get(botuid).getPosition().y * width + allBots.get(botuid).getPosition().x] = (255 << 24) | (mapred << 16) | (0) | 255;
                }
//                    else if (viewMode == VIEW_MODE_FAMILY) {
//                        rgb[currentbot.y * width + currentbot.x] = currentbot.c_family;
//                    }
                population++;
            }
            catch(Exception e) {

            }
            // show map objects
        }

//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                if (checkBotAtPos(new MapPosition(x,y))) {
//                    if (viewMode == Consts.VIEW_MODE_BASE) {
//                        rgb[y * width + x] = allBots.get(mapBotsLayer[x][y]).getColor();
//                    }
//                    else if (viewMode == Consts.VIEW_MODE_HP) {
//                        mapgreen = 255 - (int) (allBots.get(mapBotsLayer[x][y]).getHP() * 0.25);
//                        if (mapgreen < 0) mapgreen = 0;
//                        rgb[y * width + x] = (255 << 24) | (255 << 16) | (mapgreen << 8);
//                    }
//                    else if (viewMode == Consts.VIEW_MODE_MINERAL) {
//                        mapblue = 255 - (int) (currentbot.mineral * 0.5);
//                        if (mapblue < 0) mapblue = 0;
//                        rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (0) | (255 << 8) | mapblue;
//                    }
//                    else if (viewMode == VIEW_MODE_COMBINED) {
//                        mapgreen = (int) (currentbot.c_green * (1 - currentbot.health * 0.0005));
//                        if (mapgreen < 0) mapgreen = 0;
//                        mapblue = (int) (currentbot.c_blue * (0.8 - currentbot.mineral * 0.0005));
//                        rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (mapgreen << 8) | mapblue;
//                    }
//                    else if (viewMode == VIEW_MODE_AGE) {
//                        mapred = 255 - (int) (Math.sqrt(currentbot.age) * 4);
//                        if (mapred < 0) mapred = 0;
//                        rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (mapred << 16) | (0) | 255;
//                    }
//                    else if (viewMode == VIEW_MODE_FAMILY) {
//                        rgb[currentbot.y * width + currentbot.x] = currentbot.c_family;
//                    }
//                    population++;
//                }
                // show map objects
//            }
//        }

//        while (currentbot != zerobot) {
//            if (currentbot.alive == 3) {                      // живой бот
//                if (viewMode == Consts.VIEW_MODE_BASE) {
//                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (currentbot.c_green << 8) | currentbot.c_blue;
//                }
//                else if (viewMode == Consts.VIEW_MODE_HP) {
//                    mapgreen = 255 - (int) (currentbot.health * 0.25);
//                    if (mapgreen < 0) mapgreen = 0;
//                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (255 << 16) | (mapgreen << 8);
//                }
//                else if (viewMode == VIEW_MODE_MINERAL) {
//                    mapblue = 255 - (int) (currentbot.mineral * 0.5);
//                    if (mapblue < 0) mapblue = 0;
//                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (0) | (255 << 8) | mapblue;
//                }
//                else if (viewMode == VIEW_MODE_COMBINED) {
//                    mapgreen = (int) (currentbot.c_green * (1 - currentbot.health * 0.0005));
//                    if (mapgreen < 0) mapgreen = 0;
//                    mapblue = (int) (currentbot.c_blue * (0.8 - currentbot.mineral * 0.0005));
//                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (currentbot.c_red << 16) | (mapgreen << 8) | mapblue;
//                }
//                else if (viewMode == VIEW_MODE_AGE) {
//                    mapred = 255 - (int) (Math.sqrt(currentbot.age) * 4);
//                    if (mapred < 0) mapred = 0;
//                    rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (mapred << 16) | (0) | 255;
//                }
//                else if (viewMode == VIEW_MODE_FAMILY) {
//                    rgb[currentbot.y * width + currentbot.x] = currentbot.c_family;
//                }
//                population++;
//            } else if (currentbot.alive == 1) {                                            // органика, известняк, коралловые рифы
//                if (map[currentbot.x][currentbot.y] < sealevel) {                     // подводная часть
//                    mapred = 20;
//                    mapblue = 160 - (sealevel - map[currentbot.x][currentbot.y]) * 2;
//                    mapgreen = 170 - (sealevel - map[currentbot.x][currentbot.y]) * 4;
//                    if (mapblue < 40) mapblue = 40;
//                    if (mapgreen < 20) mapgreen = 20;
//                } else {                                    // скелетики, трупики на суше
//                    mapred = (int) (80 + (map[currentbot.x][currentbot.y] - sealevel) * 2.5);   // надводная часть
//                    mapgreen = (int) (60 + (map[currentbot.x][currentbot.y] - sealevel) * 2.6);
//                    mapblue = 30 + (map[currentbot.x][currentbot.y] - sealevel) * 3;
//                    if (mapred > 255) mapred = 255;
//                    if (mapblue > 255) mapblue = 255;
//                    if (mapgreen > 255) mapgreen = 255;
//                }
//                rgb[currentbot.y * width + currentbot.x] = (255 << 24) | (mapred << 16) | (mapgreen << 8) | mapblue;
//                organic++;
//            }
//            currentbot = currentbot.next;
//        }
//        currentbot = currentbot.next;

        g.drawImage(image, 0, 0, null);

        gui.generationLabel.setText(" Generation: " + generation);
        gui.populationLabel.setText(" Population: " + population);
        gui.organicLabel.setText(" Organic: " + organic);
        gui.familiesLabel.setText(" Families: " + CommonConsts.familiesCount.size());
//        gui.strainsLabel.setText(" Stable: " + stableStrainCount);
//        gui.deadStrainsLabel.setText(" Dead: " + deadStrainCount);
        gui.mutationsLabel.setText(" Mutations: " + getMutation_count());

        gui.buffer = buf;
        gui.canvas.repaint();
    }


    class Worker extends Thread {
        public void run() {
            started = true;         // Флаг работы потока, если false  поток заканчивает работу
            while (started) {       // обновляем матрицу
                long time1 = System.currentTimeMillis();

                if (botsLinkedList.size() == 0){
                    population = 0;
                    JOptionPane.showMessageDialog(gui, "All Dead!!!");
                    started = false;        // Закончили работу
                    return;
                }


                    Iterator<UUID> it = allBots.keySet().iterator();
                    while (it.hasNext()) {
                        try {
                        UUID botuid = it.next();
                        allBots.get(botuid).step();
                        }
                        catch (Exception e) {

                        }
                    }

                generation++;
                long time2 = System.currentTimeMillis();
//                System.out.println("Step execute " + ": " + (time2-time1) + "");
                if (generation % drawstep == 0) {             // отрисовка на экран через каждые ... шагов
                    paintMapView();
                    paint1();                           // отображаем текущее состояние симуляции на экран
                }
                long time3 = System.currentTimeMillis();

//                for (Strain s : AllStrains)
//                    if (!s.isAliveStrain() && s.isStableStrain()) strainDied(s);
//
//                Predicate<Strain> isDead = strain -> !strain.isAliveStrain();
//                AllStrains.removeIf(isDead);
//
//                stableStrainCount = (int) AllStrains.stream().filter(Strain::isStableStrain).count();
//                deadStrainCount = deadStrains.size();
//                System.out.println("Paint: " + (time3-time2));
            }
            started = false;        // Закончили работу
        }
    }

    public static void main(String[] args) {
        System.out.println("V2");
        instance = new WorldV2();
    }

}
