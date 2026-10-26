package com.goreecloud.launcher;

import java.util.Set;

/** Pure policy: invalidate icons after updates only for packages in the app catalog. */
final class CatalogRefreshPolicy {
    private CatalogRefreshPolicy() {}

    static boolean updatedVisiblePackage(Set<String> changedPackages, Set<String> catalogPackages) {
        if (changedPackages == null || catalogPackages == null) return false;
        for (String changed : changedPackages) {
            if (changed != null && catalogPackages.contains(changed)) return true;
        }
        return false;
    }
}
