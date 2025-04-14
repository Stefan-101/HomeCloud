// just some path traversal prevention, other tricks can still be pulled

package model;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class SecurePath {
    private final Path contextDir;
    private final Path path;

    public SecurePath(String serverStorageDir, String path) {
        this.contextDir = Paths.get(serverStorageDir).normalize();

        Path resolvedPath = this.contextDir.resolve(path).normalize();

        if (!resolvedPath.startsWith(this.contextDir)) {
            throw new IllegalArgumentException("Access denied to path: " + resolvedPath);
        }

        this.path = resolvedPath;
    }

    public Path getPath() {
        return path;
    }

    public String getStringPath(){
        return path.toString();
    }
}
