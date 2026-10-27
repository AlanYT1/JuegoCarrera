package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PantallaJuego extends ApplicationAdapter implements NetworkListener {

    public enum EstadoJuego {
        MENU,
        OPCIONES,
        JUGANDO,
        GAME_OVER,
        VIDEO_SECRETO
    }

    private final int[][] pasosKonami = {
            {Input.Keys.UP, Input.Keys.W},
            {Input.Keys.UP, Input.Keys.W},
            {Input.Keys.DOWN, Input.Keys.S},
            {Input.Keys.DOWN, Input.Keys.S},
            {Input.Keys.LEFT, Input.Keys.A},
            {Input.Keys.RIGHT, Input.Keys.D},
            {Input.Keys.LEFT, Input.Keys.A},
            {Input.Keys.RIGHT, Input.Keys.D},
            {Input.Keys.B, Input.Keys.B},
            {Input.Keys.A, Input.Keys.A}
    };
    private int indiceKonami = 0;

    private Animation<TextureRegion> animacionVideoSecreto;
    private float tiempoVideo = 0f;
    private Music musicaVideo;

    private boolean votandoReiniciar = false;
    private boolean yoVoteReiniciar = false;
    private int votosReinicioActuales = 0;
    private int totalJugadoresConectados = 1;

    private EstadoJuego estadoActual = EstadoJuego.MENU;

    private static final float SALIDA_X = 190f;
    private static final float SALIDA_Y = 257f;
    private static final float ANGULO_INICIAL = 270f;
    private static final int TOTAL_VUELTAS = 3;

    private Rectangle metaBox;
    private Rectangle checkpointBox1;
    private Rectangle checkpointBox2;
    private Rectangle checkpointBox3;
    private Rectangle checkpointBox4;

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private BitmapFont font;
    private Texture blancoTexture;

    private com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> animacionFondo;
    private float tiempoTranscurrido = 0f;
    private Music musicaMenu;

    private Texture pistaTexture;
    private Pixmap pistaPixmap;
    private Car autoLocal;
    private HiloCliente clienteRed;
    private int miId = -1;

    private final String[] SPRITES_DISPONIBLES = {
            "auto1.png", "auto2.png", "auto3.png",
            "auto4.png"
    };

    private final Map<String, Texture> texturas = new HashMap<>();
    private final Map<Integer, Car> oponentes = new ConcurrentHashMap<>();

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

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        blancoTexture = new Texture(pixmap);
        pixmap.dispose();

        pistaTexture = new Texture(Gdx.files.internal("MAPA1.png"));
        pistaPixmap = new Pixmap(Gdx.files.internal("MAPA1.png"));

        for (String sprite : SPRITES_DISPONIBLES) {
            texturas.put(sprite, new Texture(Gdx.files.internal(sprite)));
        }

        metaBox = new Rectangle(120, 203, 81, 36);
        checkpointBox1 = new Rectangle(233, 458, 19, 88);
        checkpointBox2 = new Rectangle(377, 160, 13, 75);
        checkpointBox3 = new Rectangle(606, 385, 21, 94);
        checkpointBox4 = new Rectangle(374, 9, 21, 96);

        btnJugar = new Rectangle(300, 330, 200, 45);
        btnOpciones = new Rectangle(300, 260, 200, 45);
        btnSalirMenu = new Rectangle(300, 190, 200, 45);

        btnVolverOpciones = new Rectangle(300, 200, 200, 45);

        int totalFrames = 296;
        com.badlogic.gdx.utils.Array<TextureRegion> arrFrames = new com.badlogic.gdx.utils.Array<>();

        for (int i = 1; i <= totalFrames; i++) {
            String numeroFormateado = String.format("%03d", i);
            String ruta = "El_fin_se_acerca_frames/frame_" + numeroFormateado + ".png";

            Texture texturaFrame = new Texture(Gdx.files.internal(ruta));
            arrFrames.add(new TextureRegion(texturaFrame));
        }

        float duracionTotalVideo = 11f;
        float velocidadFrame = duracionTotalVideo / totalFrames;

        animacionFondo = new com.badlogic.gdx.graphics.g2d.Animation<>(velocidadFrame, arrFrames);
        animacionFondo.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
        musicaMenu = Gdx.audio.newMusic(Gdx.files.internal("elfinseacerca.mp3"));

        musicaMenu.setLooping(true);
        musicaMenu.setVolume(5f);
        musicaMenu.play();

        int totalFramesVideo = 462;
        Array<TextureRegion> framesVideo = new Array<>();

        for (int i = 1; i <= totalFramesVideo; i++) {
            String num = String.format("%03d", i);
            Texture tex = new Texture(Gdx.files.internal("mecha_mecha_frames/frame_" + num + ".png"));
            framesVideo.add(new TextureRegion(tex));
        }
        animacionVideoSecreto = new Animation<>(11f / totalFramesVideo, framesVideo);
        animacionVideoSecreto.setPlayMode(Animation.PlayMode.LOOP);

        musicaVideo = Gdx.audio.newMusic(Gdx.files.internal("mechamecha.mp3"));

        btnReiniciar = new Rectangle(250, 280, 300, 45);
        btnSalirGameOver = new Rectangle(250, 210, 300, 45);

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
        reiniciarAutoLocal();
    }

    @Override
    public void onMovimientoRecibido(int idJugador, String datos) {

        if (datos.startsWith("VOTO_REINICIAR")) {
            votosReinicioActuales++;
            if (votosReinicioActuales >= totalJugadoresConectados) {
                ejecutarReiniciarGlobal();
            }
            return;
        }

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
        totalJugadoresConectados = Math.max(totalJugadoresConectados, idJugador);
    }

    private boolean esAsfalto(float x, float y) {
        int pixelX = (int) (x * pistaPixmap.getWidth() / 800f);
        int pixelY = (int) ((600f - y) * pistaPixmap.getHeight() / 600f);

        if (pixelX < 0 || pixelX >= pistaPixmap.getWidth() || pixelY < 0 || pixelY >= pistaPixmap.getHeight()) {
            return true;
        }

        int val = pistaPixmap.getPixel(pixelX, pixelY);

        float r = ((val >> 24) & 0xFF) / 255f;
        float g = ((val >> 16) & 0xFF) / 255f;
        float b = ((val >> 8) & 0xFF) / 255f;

        return (g < r + 0.1f) && (g < b + 0.1f);
    }

    private void reiniciarAutoLocal() {
        if (autoLocal != null) {
            int offsetId = (miId != -1) ? miId - 1 : 0;
            autoLocal.x = SALIDA_X - (offsetId * 50f);
            autoLocal.y = SALIDA_Y;
            autoLocal.angle = ANGULO_INICIAL;
            autoLocal.speed = 0;
            autoLocal.lap = 1;
            autoLocal.checkpointPassed1 = false;
            autoLocal.checkpointPassed2 = false;
            autoLocal.checkpointPassed3 = false;
            autoLocal.checkpointPassed4 = false;
        }
    }

    private void ejecutarReiniciarGlobal() {
        if (autoLocal != null) {
            autoLocal.detener();
        }
        reiniciarAutoLocal();
        estadoActual = EstadoJuego.JUGANDO;
        yoVoteReiniciar = false;
        votosReinicioActuales = 0;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            togglePantallaCompleta();
        }

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
            case VIDEO_SECRETO:
                renderVideoSecreto(mouse, click);
                break;
        }

        batch.end();
    }

    private void renderVideoSecreto(Vector3 mouse, boolean click) {
        tiempoVideo += Gdx.graphics.getDeltaTime();
        TextureRegion frameVideo = animacionVideoSecreto.getKeyFrame(tiempoVideo, true);

        batch.draw(frameVideo, 0, 0, 800, 600);

        font.setColor(Color.WHITE);
        font.draw(batch, "Presiona [ESC] para volver al Menú", 20, 570);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (musicaVideo.isPlaying()) {
                musicaVideo.stop();
            }
            if (!musicaMenu.isPlaying()) {
                musicaMenu.play();
            }
            estadoActual = EstadoJuego.MENU;
        }
    }

    private void renderMenu(Vector3 mouse, boolean click) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            int teclaPresionada = -1;

            for (int i = 0; i < 256; i++) {
                if (Gdx.input.isKeyJustPressed(i)) {
                    teclaPresionada = i;
                    break;
                }
            }

            if (teclaPresionada != -1) {
                boolean esValida = false;
                for (int teclaPermitida : pasosKonami[indiceKonami]) {
                    if (teclaPresionada == teclaPermitida) {
                        esValida = true;
                        break;
                    }
                }

                if (esValida) {
                    indiceKonami++;
                    if (indiceKonami == pasosKonami.length) {
                        estadoActual = EstadoJuego.VIDEO_SECRETO;
                        tiempoVideo = 0f;
                        indiceKonami = 0;

                        if (musicaMenu.isPlaying()) musicaMenu.pause();
                        musicaVideo.play();
                    }
                } else {
                    boolean esPrimerPaso = false;
                    for (int teclaPermitida : pasosKonami[0]) {
                        if (teclaPresionada == teclaPermitida) {
                            esPrimerPaso = true;
                            break;
                        }
                    }
                    indiceKonami = esPrimerPaso ? 1 : 0;
                }
            }
        }

        tiempoTranscurrido += Gdx.graphics.getDeltaTime();
        TextureRegion frameActual = animacionFondo.getKeyFrame(tiempoTranscurrido, true);
        batch.draw(frameActual, 0, 0, 800, 600);

        font.getData().setScale(2.3f);
        font.setColor(Color.GOLD);
        font.draw(batch, "Pimpollo", 333, 480);
        font.getData().setScale(1.0f);

        if (dibujaBoton("JUGAR", btnJugar, Color.DARK_GRAY, Color.WHITE, mouse, click)) {
            reiniciarAutoLocal();
            estadoActual = EstadoJuego.JUGANDO;

            if (musicaMenu.isPlaying()){
                musicaMenu.stop();
            }
        }

        if (dibujaBoton("OPCIONES", btnOpciones, Color.DARK_GRAY, Color.WHITE, mouse, click)) {
            estadoActual = EstadoJuego.OPCIONES;
        }

        if (dibujaBoton("SALIR", btnSalirMenu, Color.FIREBRICK, Color.WHITE, mouse, click)) {
            Gdx.app.exit();
        }
    }

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

    private void renderJuego(float delta) {

        if (Gdx.input.justTouched()) {
            Vector3 mouse = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            System.out.println("SALIDA_X = " + mouse.x + "f;  SALIDA_Y = " + mouse.y + "f;");
        }

        if (autoLocal != null) {
            boolean up = Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W);
            boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
            boolean left = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
            boolean right = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);

            boolean enPasto = !esAsfalto(autoLocal.x, autoLocal.y);

            autoLocal.update(delta, up, down, left, right, enPasto);

            Rectangle autoBox = new Rectangle(autoLocal.x - 12, autoLocal.y - 12, 24, 24);

            if (autoBox.overlaps(checkpointBox1)) {
                autoLocal.checkpointPassed1 = true;
            }
            if (autoBox.overlaps(checkpointBox2)) {
                autoLocal.checkpointPassed2 = true;
            }
            if (autoBox.overlaps(checkpointBox3)) {
                autoLocal.checkpointPassed3 = true;
            }
            if (autoBox.overlaps(checkpointBox4)) {
                autoLocal.checkpointPassed4 = true;
            }

            if (autoLocal.checkpointPassed1 && autoLocal.checkpointPassed2 && autoLocal.checkpointPassed3 && autoLocal.checkpointPassed4 && autoBox.overlaps(metaBox)) {
                if (autoLocal.lap < TOTAL_VUELTAS) {
                    autoLocal.lap++;
                    autoLocal.checkpointPassed1 = false;
                    autoLocal.checkpointPassed2 = false;
                    autoLocal.checkpointPassed3 = false;

                } else {
                    autoLocal.speed = 0;
                    autoLocal.detener();
                    estadoActual = EstadoJuego.GAME_OVER;
                }
            }else if ((autoLocal.checkpointPassed1 || autoLocal.checkpointPassed2 || autoLocal.checkpointPassed3 || autoLocal.checkpointPassed4) && autoBox.overlaps(metaBox)) {
                autoLocal.checkpointPassed1 = false;
                autoLocal.checkpointPassed2 = false;
                autoLocal.checkpointPassed3 = false;
                autoLocal.checkpointPassed4 = false;
            }

            if (clienteRed != null && miId != -1) {
                clienteRed.enviarMensaje(String.format(Locale.US, "POS:%d:%.2f:%.2f:%.2f:%s:%d",
                        miId, autoLocal.x, autoLocal.y, autoLocal.angle, autoLocal.spriteName, autoLocal.lap));
            }
        }

        dibujarEscenaCarrera();
    }

    private void renderGameOver(Vector3 mouse, boolean click) {
        dibujarEscenaCarrera();

        batch.setColor(0, 0, 0, 0.75f);
        batch.draw(blancoTexture, 180, 150, 440, 300);
        batch.setColor(Color.WHITE);

        font.getData().setScale(1.8f);
        font.setColor(Color.GOLD);
        font.draw(batch, "¡CARRERA FINALIZADA!", 220, 390);
        font.getData().setScale(1.0f);

        if (dibujaBoton("REINICIAR CARRERA", btnReiniciar, Color.GREEN, Color.WHITE, mouse, click)) {
            if (!yoVoteReiniciar) {
                yoVoteReiniciar = true;
                votosReinicioActuales++;

                if (clienteRed != null) {
                    clienteRed.enviarMensaje("VOTO_REINICIAR:" + miId);
                }
            }

            if (yoVoteReiniciar) {
                font.setColor(Color.YELLOW);
                font.draw(batch, "Esperando votos... (" + votosReinicioActuales + " / " + totalJugadoresConectados + ")", 250, 260);
            }

            reiniciarAutoLocal();
            estadoActual = EstadoJuego.JUGANDO;
        }

        if (dibujaBoton("SALIR AL MENÚ", btnSalirGameOver, Color.FIREBRICK, Color.WHITE, mouse, click)) {
            if (autoLocal != null) {
                autoLocal.detener();
            }
            yoVoteReiniciar = false;
            votosReinicioActuales = 0;
            if (!musicaMenu.isPlaying()) {
                musicaMenu.play();
            }
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

    private boolean dibujaBoton(String texto, Rectangle rect, Color colorBase, Color colorTexto, Vector3 mouse, boolean click) {
        boolean hover = rect.contains(mouse.x, mouse.y);

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
        if (pistaPixmap != null) pistaPixmap.dispose();
        if (blancoTexture != null) blancoTexture.dispose();
        if (font != null) font.dispose();
        for (Texture t : texturas.values()) {
            t.dispose();
        }
        if (musicaVideo != null) {
            musicaVideo.dispose();
        }
        if (animacionVideoSecreto != null) {
            for (TextureRegion region : animacionVideoSecreto.getKeyFrames()) {
                region.getTexture().dispose();
            }
        }
    }
}