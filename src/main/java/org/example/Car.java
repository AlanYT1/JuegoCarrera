package org.example;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Car {
    public float x, y;
    public float angle;
    public float speed = 0;
    public String spriteName;

    public static final float MAX_SPEED = 200f;
    public static final float ACCELERATION = 50f;
    public static final float ROTATION_SPEED = 90f;

    public Car(float x, float y, String spriteName) {
        this.x = x;
        this.y = y;
        this.angle = 0;
        this.spriteName = spriteName;
    }

    public void update(float delta, boolean up, boolean down, boolean left, boolean right) {
        // Aceleración y marcha atrás
        if (up) {
            speed = Math.min(MAX_SPEED, speed + ACCELERATION * delta);
        } else if (down) {
            speed = Math.max(-MAX_SPEED / 2, speed - ACCELERATION * delta);
        } else {
            speed *= 0.98f; // Fricción
        }

        // Giro (solo gira cuando el vehículo está en movimiento)
        if (Math.abs(speed) > 10) {
            float dir = speed > 0 ? 1 : -1;
            if (left) angle += ROTATION_SPEED * delta * dir;
            if (right) angle -= ROTATION_SPEED * delta * dir;
        }

        // Avance según el ángulo de orientación
        x += MathUtils.cosDeg(angle) * speed * delta;
        y += MathUtils.sinDeg(angle) * speed * delta;
    }

    public void render(SpriteBatch batch, Texture texture) {
        float originX = texture.getWidth() / 2f;
        float originY = texture.getHeight() / 2f;

        batch.draw(
                texture,
                x - originX, y - originY,
                originX, originY,
                texture.getWidth(), texture.getHeight(),
                0.5f, 0.5f,             // Escala en X e Y
                angle,              // Ángulo en grados
                0, 0,
                texture.getWidth(), texture.getHeight(),
                false, false
        );
    }
}