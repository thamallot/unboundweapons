package org.minitype.mcmodstest.token;

public enum TokenMode {
    SMP("smp"),
    SINGLEPLAYER("singleplayer"),
    HYBRID("hybrid");

    private final String id;

    TokenMode(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TokenMode fromId(String id) {
        for (TokenMode mode : values()) {
            if (mode.id.equalsIgnoreCase(id)) {
                return mode;
            }
        }

        return HYBRID;
    }
}
