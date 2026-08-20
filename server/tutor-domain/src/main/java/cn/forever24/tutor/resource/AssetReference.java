package cn.forever24.tutor.resource;

public record AssetReference(String assetKey, int displayOrder) {

    public AssetReference {
        assetKey = ResourceValidation.required(assetKey, "asset reference key");
        if (displayOrder < 0) {
            throw new IllegalArgumentException("asset display order cannot be negative");
        }
    }
}
