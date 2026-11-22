package dev.austin.christmas.scene;

/** Shared landing surface for both the snowfall and the rendered snow bank. */
final class SnowGround {
    static double surface(double x, int width, int height) {
        double u = x / Math.max(1, width);
        return height * .815 + Math.min(4, height * .006)
                * (Math.sin(u * Math.PI * 3) + .35 * Math.sin(u * Math.PI * 9));
    }
}
