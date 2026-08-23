package com.recruitment.candidateservice.config;

import java.util.UUID;

public final class DemoIds {

    /** Matches auth-service DemoIds.HR */
    public static final UUID HR_USER = UUID.fromString("10000000-0000-4000-8000-000000000002");

    public static final UUID ALICE = UUID.fromString("20000000-0000-4000-8000-000000000001");
    public static final UUID BOB = UUID.fromString("20000000-0000-4000-8000-000000000002");

    private DemoIds() {
    }
}
