package org.dreeam.sapling.version;

import org.galemc.gale.version.AbstractPaperVersionFetcher;

public class SaplingVersionFetcher extends AbstractPaperVersionFetcher {

    public SaplingVersionFetcher() {
        super(
            "https://github.com/Winds-Studio/Sapling",
            "Winds Studio",
            "Sapling",
            "Winds-Studio",
            "Sapling"
        );
    }
}
