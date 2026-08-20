package cn.forever24.tutor.application.resource;

public final class CatalogApplicationException extends RuntimeException {

    private final String code;
    private final int status;

    private CatalogApplicationException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static CatalogApplicationException badRequest(String code, String message) {
        return new CatalogApplicationException(code, 400, message);
    }

    public static CatalogApplicationException forbidden(String code, String message) {
        return new CatalogApplicationException(code, 403, message);
    }

    public static CatalogApplicationException notFound(String code, String message) {
        return new CatalogApplicationException(code, 404, message);
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }
}
