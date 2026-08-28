package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
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

    // Estados del Juego
    public enum EstadoJuego {
        MENU,
        OPCIONES,
        JUGANDO,
        GAME_OVER
    }

    private EstadoJuego estadoActual = EstadoJuego.MENU;

    private static final float SALIDA_X = 552f;
    private static final float SALIDA_Y = 408f;
    private static final float ANGULO_INICIAL = 180f;
    private static final int TOTAL_VUELTAS = 3;

    private Rectangle metaBox;
    private Rectangle checkpointBox;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;
    private Texture blancoTexture; // Textura para dibujar fondos de botones

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

    // Rectángulos de botones
    private Rectangle btnJugar;
    private Rectangle btnOpciones;
    private Rectangle btnSalirMenu;
    private Rectangle btnVolverOpciones;
    private Rectangle btnReiniciar;
    private Rectangle btnSalirGameOver;

    @Override
    public void create() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        camera.position.set(400, 300, 0);

        font = new BitmapFont();

        // Crear textura de 1x1 píxel blanco para los rectángulos de los botones
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        blancoTexture = new Texture(pixmap);
        pixmap.dispose();

        pistaTexture = new Texture(Gdx.files.internal("pista.png"));

        for (String sprite : SPRITES_DISPONIBLES) {
            texturas.put(sprite, new Texture(Gdx.files.internal(sprite)));
        }

        // Definir zonas
        metaBox = new Rectangle(SALIDA_X - 48, SALIDA_Y - 25, 20, 127);
        checkpointBox = new Rectangle(512, 94, 20, 127);

        // Inicializar posiciones de botones (X, Y, Ancho, Alto)
        btnJugar = new Rectangle(300, 330, 200, 45);
        btnOpciones = new Rectangle(300, 260, 200, 45);
        btnSalirMenu = new Rectangle(300, 190, 200, 45);

        btnVolverOpciones = new Rectangle(300, 200, 200, 45);

        btnReiniciar = new Rectangle(250, 280, 300, 45);
        btnSalirGameOver = new Rectangle(250, 210, 300, 45);

        // Auto local
        autoLocal = new Car(SALIDA_X, SALIDA_Y, SPRITES_DISPONIBLES[0]);
        autoLocal.angle = ANGULO_INICIAL;

        // Red
        clienteRed = new HiloCliente(this);
        clienteRed.start();
    }

    @Override
    public void onJugadorConectado(int id) {
        this.miId = id;
        String miSprite = SPRITES_DISPONIBLES[(id - 1) % SPRITES_DISPONIBLES.length];
        autoLocal.spriteName = miSprite;
        reiniciarAutoLocal();
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

    private void reiniciarAutoLocal() {
        if (autoLocal != null) {
            int offsetId = (miId != -1) ? miId - 1 : 0;
            autoLocal.x = SALIDA_X + (offsetId * 35f);
            autoLocal.y = SALIDA_Y;
            autoLocal.angle = ANGULO_INICIAL;
            autoLocal.speed = 0;
            autoLocal.lap = 1;
            autoLocal.checkpointPassed = false;
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // Tecla F11 para Pantalla Completa en cualquier momento
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            togglePantallaCompleta();
        }

        // Obtener coordenadas del mouse dentro del juego
        Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        boolean click = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        camera.update();
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        switch (estadoActual) {
            case MENU:
                renderMenu(mouse, click);
                break;
            case OPCIONES:
                renderOpciones(mouse, click);
                break;
            case JUGANDO:
                renderJuego(delta);
                break;
            case GAME_OVER:
                renderGameOver(mouse, click);
                break;
        }

        batch.end();
    }

    // --- MENÚ DE INICIO ---
    private void renderMenu(Vector3 mouse, boolean click) {
        font.getData().setScale(1.8f);
        font.setColor(Color.GOLD);
        font.draw(batch, "JUEGO DE CARRERAS MULTIJUGADOR", 130, 480);
        font.getData().setScale(1.0f);

        if (dibujaBoton("JUGAR", btnJugar, Color.DARK_GRAY, Color.WHITE, mouse, click)) {
            reiniciarAutoLocal();
            estadoActual = EstadoJuego.JUGANDO;
        }

        if (dibujaBoton("OPCIONES", btnOpciones, Color.DARK_GRAY, Color.WHITE, mouse, click)) {
            estadoActual = EstadoJuego.OPCIONES;
        }

        if (dibujaBoton("SALIR", btnSalirMenu, Color.FIREBRICK, Color.WHITE, mouse, click)) {
            Gdx.app.exit();
        }
    }

    // --- MENÚ DE OPCIONES ---
    private void renderOpciones(Vector3 mouse, boolean click) {
        font.getData().setScale(1.6f);
        font.setColor(Color.WHITE);
        font.draw(batch, "OPCIONES", 340, 480);
        font.getData().setScale(1.0f);

        font.draw(batch, "Presiona F11 para alternar Pantalla Completa", 260, 350);

        if (dibujaBoton("VOLVER AL MENÚ", btnVolverOpciones, Color.DARK_GRAY, Color.WHITE, mouse, click)) {
            estadoActual = EstadoJuego.MENU;
        }
    }

    // --- LÓGICA Y RENDER DEL JUEGO ---
    private void renderJuego(float delta) {

        if (Gdx.input.justTouched()) {
            Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            System.out.println("SALIDA_X = " + mouse.x + "f;  SALIDA_Y = " + mouse.y + "f;");
        }

        // Control e interacción del vehículo local
        if (autoLocal != null) {
            boolean up = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
            boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

            autoLocal.update(delta, up, down, left, right);

            // Detección de vueltas
            Rectangle autoBox = new Rectangle(autoLocal.x - 12, autoLocal.y - 12, 24, 24);

            if (autoBox.overlaps(checkpointBox)) {
                autoLocal.checkpointPassed = true;
            }

            if (autoLocal.checkpointPassed && autoBox.overlaps(metaBox)) {
                if (autoLocal.lap < TOTAL_VUELTAS) {
                    autoLocal.lap++;
                    autoLocal.checkpointPassed = false;
                } else {
                    // Completó todas las vueltas
                    estadoActual = EstadoJuego.GAME_OVER;
                }
            }

            // Enviar posición por red
            if (clienteRed != null && miId != -1) {
                clienteRed.enviarMensaje(String.format("POS:%d:%.2f:%.2f:%.2f:%s:%d",
                        miId, autoLocal.x, autoLocal.y, autoLocal.angle, autoLocal.spriteName, autoLocal.lap));
            }
        }

        dibujarEscenaCarrera();
    }

    // --- PANTALLA DE FIN DE CARRERA (GAME OVER) ---
    private void renderGameOver(Vector3 mouse, boolean click) {
        // Dibujar escena de fondo semi-transparente
        dibujarEscenaCarrera();

        // Fondo oscuro para la ventana flotante de fin de carrera
        batch.setColor(0, 0, 0, 0.75f);
        batch.draw(blancoTexture, 180, 150, 440, 300);
        batch.setColor(Color.WHITE);

        font.getData().setScale(1.8f);
        font.setColor(Color.GOLD);
        font.draw(batch, "¡CARRERA FINALIZADA!", 220, 390);
        font.getData().setScale(1.0f);

        if (dibujaBoton("REINICIAR CARRERA", btnReiniciar, Color.GREEN, Color.WHITE, mouse, click)) {
            reiniciarAutoLocal();
            estadoActual = EstadoJuego.JUGANDO;
        }

        if (dibujaBoton("SALIR AL MENÚ", btnSalirGameOver, Color.FIREBRICK, Color.WHITE, mouse, click)) {
            estadoActual = EstadoJuego.MENU;
        }
    }

    private void dibujarEscenaCarrera() {
        batch.draw(pistaTexture, 0, 0, 800, 600);

        if (autoLocal != null) {
            Texture texLocal = texturas.get(autoLocal.spriteName);
            if (texLocal != null) {
                autoLocal.render(batch, texLocal);
            }
            font.draw(batch, "Lap " + autoLocal.lap + "/" + TOTAL_VUELTAS, autoLocal.x - 20, autoLocal.y + 35);
            font.draw(batch, "Tu Vuelta: " + autoLocal.lap + " / " + TOTAL_VUELTAS, 20, 580);
        }

        for (Map.Entry<Integer, Car> entry : oponentes.entrySet()) {
            int idRival = entry.getKey();
            Car rival = entry.getValue();
            Texture texRival = texturas.get(rival.spriteName);
            if (texRival != null) {
                rival.render(batch, texRival);
            }
            font.draw(batch, "P" + idRival + " Lap " + rival.lap + "/" + TOTAL_VUELTAS, rival.x - 25, rival.y + 35);
        }
    }

    // --- DIBUJAR BOTONES CON DETECCIÓN DE MOUSE ---
    private boolean dibujaBoton(String texto, Rectangle rect, Color colorBase, Color colorTexto, Vector3 mouse, boolean click) {
        boolean hover = rect.contains(mouse.x, mouse.y);

        // Cambiar color cuando el puntero pasa sobre el botón
        if (hover) {
            batch.setColor(colorBase.r + 0.2f, colorBase.g + 0.2f, colorBase.b + 0.2f, 1f);
        } else {
            batch.setColor(colorBase);
        }

        batch.draw(blancoTexture, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.WHITE);

        font.setColor(colorTexto);
        font.draw(batch, texto, rect.x + 35, rect.y + 28);
        font.setColor(Color.WHITE);

        return hover && click;
    }

    private void togglePantallaCompleta() {
        if (Gdx.graphics.isFullscreen()) {
            Gdx.graphics.setWindowedMode(800, 600);
        } else {
            Graphics.DisplayMode currentMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(currentMode);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (pistaTexture != null) pistaTexture.dispose();
        if (blancoTexture != null) blancoTexture.dispose();
        if (font != null) font.dispose();
        for (Texture t : texturas.values()) {
            t.dispose();
        }
    }
}