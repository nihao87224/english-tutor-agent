package cn.forever24.tutor.resource;

public record FocalPoint(double x, double y) {

    public FocalPoint {
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            throw new IllegalArgumentException("focal point coordinates must be between 0 and 1");
        }
    }
}
