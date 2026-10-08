package com.goreecloud.launcher

/**
 * Package-local aliases keep MainActivity's secondary Home movement callbacks concise while the
 * underlying movement contracts remain owned by the workspace layers that implement them.
 */
internal typealias WorkspaceMoveDirection =
    com.goreecloud.launcher.core.workspace.WorkspaceMoveDirection

internal typealias WorkspaceHomeSpatialDirection =
    com.goreecloud.launcher.core.workspace.db.WorkspaceHomeSpatialDirection
