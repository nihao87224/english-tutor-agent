package cn.forever24.tutor.application.content;

import cn.forever24.tutor.content.ContentImportIssue;
import cn.forever24.tutor.resource.ResourceCatalogEntry;

import java.util.List;
import java.util.Optional;

/** Infrastructure validates the versioned JSON contract before application code persists any catalog entry. */
public interface ContentManifestValidator {
    Validation validate(String manifestJson);

    record Validation(Optional<ResourceCatalogEntry> entry, List<ContentImportIssue> issues) {
        public Validation {
            entry = entry == null ? Optional.empty() : entry;
            issues = List.copyOf(issues == null ? List.of() : issues);
            if (entry.isPresent() == !issues.isEmpty()) {
                throw new IllegalArgumentException("content validation must be either valid or have issues");
            }
        }
        public static Validation valid(ResourceCatalogEntry entry) { return new Validation(Optional.of(entry), List.of()); }
        public static Validation invalid(List<ContentImportIssue> issues) { return new Validation(Optional.empty(), issues); }
    }
}
