package cn.forever24.tutor.ai.provider;

public record AsrOptions(
        String languageTag,
        boolean enableWordTimestamps
) {

    public AsrOptions {
        languageTag = ProviderText.requireNonBlank(languageTag, "languageTag");
    }

    public static AsrOptions english() {
        return new AsrOptions("en-US", false);
    }
}
