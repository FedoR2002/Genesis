public interface GuiCallback {
    void drawStepChanged(int value);
    void mapGenerationStarted(int canvasWidth, int canvasHeight);
    void seaLevelChanged(int value);
    boolean startedOrStopped();
    void viewModeBotChanged(int viewModeBot);
    void viewModeMapChanged(int viewModeMap);
    void setWorldScale(int worldScale);
    void setPerlin(int perlin);
}
