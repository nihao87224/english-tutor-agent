package cn.forever24.tutor.resource;

public record ContentProvider(String providerCode, String displayName, ContentProviderType type) {

    public ContentProvider {
        providerCode = ResourceValidation.required(providerCode, "providerCode");
        displayName = ResourceValidation.required(displayName, "displayName");
        if (type == null) {
            throw new IllegalArgumentException("provider type is required");
        }
    }
}
