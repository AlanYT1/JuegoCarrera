package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PantallaJuego extends ApplicationAdapter implements NetworkListener {

    private static final float SALIDA_X = 552f;
    private static final float SALIDA_Y = 408f;
    private static final float ANGULO_INICIAL = 180f;

    // Número total de vueltas de la carrera
    private static final int TOTAL_VUELTAS = 3;

    // Rectángulos para detectar la meta y el punto medio
    private Rectangle metaBox;
    private Rectangle checkpointBox;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;

    private Texture pistaTexture;

    private Car autoLocal;
    private HiloCliente clienteRed;
    private int miId = -1;

    private final String[] SPRITES_DISPONIBLES = {
            "BlackOut.png", "BlueStrip.png", "GreenStrip.png",
            "PinkStrip.png", "RedStrip.png", "WhiteStrip.png"
    };

    private final Map<String, Texture> texturas = new HashMap<>();
    private final Map<Integer, Car> oponentes = new ConcurrentHashMap<>();

    @Override
    public void create() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        camera.position.set(400, 300, 0);

        font = new BitmapFont(); // Fuente predeterminada de LibGDX

        pistaTexture = new Texture(Gdx.files.internal("pista.png"));

        for (String sprite : SPRITES_DISPONIBLES) {
            texturas.put(sprite, new Texture(Gdx.files.internal(sprite)));
        }

        // Definir zona de meta (sobre SALIDA_X, SALIDA_Y)
        metaBox = new Rectangle(SALIDA_X - 20, SALIDA_Y - 30, 40, 60);

        // Definir punto de control al otro lado de la pista (ajusta X e Y si es necesario)
        checkpointBox = new Rectangle(521, 94, 80, 80);

        autoLocal = new Car(SALIDA_X, SALIDA_Y, SPRITES_DISPONIBLES[0]);
        autoLocal.angle = ANGULO_INICIAL;

        clienteRed = new HiloCliente(this);
        clienteRed.start();
    }

    @Override
    public void onJugadorConectado(int id) {
        this.miId = id;

        String miSprite = SPRITES_DISPONIBLES[(id - 1) % SPRITES_DISPONIBLES.length];
        autoLocal.spriteName = miSprite;

        autoLocal.x = SALIDA_X + ((id - 1) * 35f);
        autoLocal.y = SALIDA_Y;
        autoLocal.angle = ANGULO_INICIAL;
    }

    @Override
    public void onMovimientoRecibido(int idJugador, String datos) {
        String[] partes = datos.split(":");
        if (partes.length < 4) return;

        float x = Float.parseFloat(partes[0]);
        float y = Float.parseFloat(partes[1]);
        float angle = Float.parseFloat(partes[2]);
        String spriteRival = partes[3];
        int lapRival = (partes.length >= 5) ? Integer.parseInt(partes[4]) : 1;

        Car rival = oponentes.get(idJugador);
        if (rival == null) {
            rival = new Car(x, y, spriteRival);
            rival.lap = lapRival;
            oponentes.put(idJugador, rival);
        } else {
            rival.x = x;
            rival.y = y;
            rival.angle = angle;
            rival.spriteName = spriteRival;
            rival.lap = lapRival;
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(800, 600);
            } else {
                Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setFullscreenMode(currentMode);
            }
        }

        if (autoLocal != null) {
            boolean up = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
            boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

            autoLocal.update(delta, up, down, left, right);

            // Detección de vuelta
            Rectangle autoBox = new Rectangle(autoLocal.x - 12, autoLocal.y - 12, 24, 24);

            if (autoBox.overlaps(checkpointBox)) {
                autoLocal.checkpointPassed = true;
            }

            if (autoLocal.checkpointPassed && autoBox.overlaps(metaBox)) {
                if (autoLocal.lap < TOTAL_VUELTAS) {
                    autoLocal.lap++;
                    autoLocal.checkpointPassed = false;
                }
            }

            // Envío por red incluyendo la vuelta
            if (clienteRed != null && miId != -1) {
                clienteRed.enviarMensaje(String.format("POS:%d:%.2f:%.2f:%.2f:%s:%d",
                        miId, autoLocal.x, autoLocal.y, autoLocal.angle, autoLocal.spriteName, autoLocal.lap));
            }
        }

        if (Gdx.input.justTouched()) {
            Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            System.out.println("Clic en X: " + mouse.x + " | Y: " + mouse.y);
        }

        // Renderizado
        camera.update();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 1. Pista
        batch.draw(pistaTexture, 0, 0, 800, 600);

        // 2. Autos
        if (autoLocal != null) {
            Texture texLocal = texturas.get(autoLocal.spriteName);
            if (texLocal != null) {
                autoLocal.render(batch, texLocal);
            }
        }

        for (Car rival : oponentes.values()) {
            Texture texRival = texturas.get(rival.spriteName);
            if (texRival != null) {
                rival.render(batch, texRival);
            }
        }

        // 3. Renderizado del texto de las vueltas
        if (autoLocal != null) {
            // Etiqueta sobre el auto del jugador local
            font.draw(batch, "Lap " + autoLocal.lap + "/" + TOTAL_VUELTAS, autoLocal.x - 20, autoLocal.y + 35);

            // HUD fijo superior
            font.draw(batch, "Tu Vuelta: " + autoLocal.lap + " / " + TOTAL_VUELTAS, 20, 580);

            if (autoLocal.lap >= TOTAL_VUELTAS && autoLocal.checkpointPassed) {
                font.draw(batch, "¡CARRERA FINALIZADA!", 320, 580);
            }
        }

        // Etiqueta flotante sobre cada oponente
        for (Map.Entry<Integer, Car> entry : oponentes.entrySet()) {
            int idRival = entry.getKey();
            Car rival = entry.getValue();
            font.draw(batch, "P" + idRival + " Lap " + rival.lap + "/" + TOTAL_VUELTAS, rival.x - 25, rival.y + 35);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (pistaTexture != null) pistaTexture.dispose();
        if (font != null) font.dispose();
        for (Texture t : texturas.values()) {
            t.dispose();
        }
    }
}