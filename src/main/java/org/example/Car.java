package org.example;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public class Car {
    public float x, y;
    public float angle;
    public float speed = 0;
    public String spriteName;

    public int lap = 1;
    public boolean checkpointPassed1 = false;
    public boolean checkpointPassed2 = false;
    public boolean checkpointPassed3 = false;
    public boolean checkpointPassed4 = false;

    Sound sound = Gdx.audio.newSound(Gdx.files.internal("f1.mp3"));

    private long soundId = -1;
    private boolean isPlayingSound = false;

    private long soundReverseId = -1;
    private boolean isPlayingReverse = false;

    public static final float MAX_SPEED = 200f;
    public static final float ACCELERATION = 50f;
    public static final float ROTATION_SPEED = 90f;

    public Car(float x, float y, String spriteName) {
        this.x = x;
        this.y = y;
        this.angle = 0;
        this.spriteName = spriteName;
    }

    public void update(float delta, boolean up, boolean down, boolean left, boolean right, boolean enPasto) {
        float maxVelActual = enPasto ? MAX_SPEED * 0.3f : MAX_SPEED;
        float aceleracionActual = enPasto ? ACCELERATION * 0.3f : ACCELERATION;

        if (up) {
            speed = Math.min(MAX_SPEED, speed + ACCELERATION * delta);

            if (!isPlayingSound) {
                soundId = sound.play(0.5f);
                sound.setPitch(soundId, 0.67f);
                sound.setLooping(soundId, true);
                isPlayingSound = true;
            }
        } else {
            if (isPlayingSound) {
                sound.stop(soundId);
                isPlayingSound = false;
            }
        }

        if (down) {
            speed = Math.max(-maxVelActual / 2, speed - aceleracionActual * delta);

            if (!isPlayingReverse) {
                soundReverseId = sound.play(0.5f);
                sound.setPitch(soundReverseId, 0.3f);
                sound.setLooping(soundReverseId, true);
                isPlayingReverse = true;
            }
        } else {
            if (isPlayingReverse) {
                sound.stop(soundReverseId);
                isPlayingReverse = false;
            }
        }

        if (!up && !down) {
            speed *= 0.98f;
        }

        if (enPasto && Math.abs(speed) > maxVelActual) {
            speed *= 0.92f;
        }

        if (Math.abs(speed) > 10) {
            float dir = speed > 0 ? 1 : -1;
            if (left) angle += ROTATION_SPEED * delta * dir;
            if (right) angle -= ROTATION_SPEED * delta * dir;
        }

        x += MathUtils.cosDeg(angle) * speed * delta;
        y += MathUtils.sinDeg(angle) * speed * delta;
    }

    public void render(SpriteBatch batch, Texture texture) {
        float anchoCarro = 42f;
        float altoCarro = 30f;

        float origenX = anchoCarro / 2f;
        float origenY = altoCarro / 2f;

        float anguloCorregido = this.angle + 180f;

        batch.draw(
                new TextureRegion(texture),
                x - origenX, y - origenY,
                origenX, origenY,
                anchoCarro, altoCarro,
                1.0f, 1.0f,
                anguloCorregido
        );
    }

    public void detener() {
        this.speed = 0;
        if (sound != null) {
            sound.stop();
        }
    }
}