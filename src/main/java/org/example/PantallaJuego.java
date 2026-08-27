package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PantallaJuego extends ApplicationAdapter implements NetworkListener {

    // Definir las coordenadas exactas de la línea de salida de tu pista
    private static final float SALIDA_X = 552f; // Ajustá según la X de tu meta
    private static final float SALIDA_Y = 408f; // Ajustá según la Y de tu meta
    private static final float ANGULO_INICIAL = 180f; // 0° apunta a la derecha, 90° arriba, 180° izquierda, 270° abajo


    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;

    // Textura para el fondo del circuito
    private Texture pistaTexture;

    private Car autoLocal;
    private HiloCliente clienteRed;

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

        pistaTexture = new Texture(Gdx.files.internal("pista.png"));

        for (String sprite : SPRITES_DISPONIBLES) {
            texturas.put(sprite, new Texture(Gdx.files.internal(sprite)));
        }

        // 1. Aparecer en la línea de salida por defecto
        autoLocal = new Car(SALIDA_X, SALIDA_Y, SPRITES_DISPONIBLES[0]);
        autoLocal.angle = ANGULO_INICIAL;

        clienteRed = new HiloCliente(this);
        clienteRed.start();
    }

    @Override
    public void onJugadorConectado(int id) {
        // Asigna un sprite diferente para cada ID de jugador
        String miSprite = SPRITES_DISPONIBLES[(id - 1) % SPRITES_DISPONIBLES.length];
        autoLocal.spriteName = miSprite;

        // Escalonar autos hacia la derecha (detrás de la línea de salida)
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

        Car rival = oponentes.get(idJugador);
        if (rival == null) {
            rival = new Car(x, y, spriteRival);
            oponentes.put(idJugador, rival);
        } else {
            rival.x = x;
            rival.y = y;
            rival.angle = angle;
            rival.spriteName = spriteRival;
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Tecla F11 para Pantalla Completa
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen()) {
                Gdx.graphics.setWindowedMode(800, 600);
            } else {
                Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
                Gdx.graphics.setFullscreenMode(currentMode);
            }
        }

        // Control del auto local
        if (autoLocal != null) {
            boolean up = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
            boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

            autoLocal.update(delta, up, down, left, right);

            if (clienteRed != null && autoLocal != null) {
                clienteRed.enviarMensaje(String.format("POS:%.2f:%.2f:%.2f:%s",
                        autoLocal.x, autoLocal.y, autoLocal.angle, autoLocal.spriteName));
            }
        }
        if (Gdx.input.justTouched()) {
            // Convierte el clic en pantalla a coordenadas de la cámara/juego (800x600)
            com.badlogic.gdx.math.Vector3 mouse = camera.unproject(new com.badlogic.gdx.math.Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            System.out.println("Línea de salida en X: " + mouse.x + " | Y: " + mouse.y);
        }


        // Renderizado
        camera.update();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 2. PRIMERO DIBUJAR LA PISTA EN EL FONDO (0, 0, 800, 600)
        batch.draw(pistaTexture, 0, 0, 800, 600);

        // 3. LUEGO DIBUJAR LOS AUTOS SOBRE LA PISTA
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
        for (Texture t : texturas.values()) {
            t.dispose();
        }
    }
}