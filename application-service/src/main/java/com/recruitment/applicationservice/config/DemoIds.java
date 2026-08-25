package com.recruitment.applicationservice.config;

import java.util.UUID;

public final class DemoIds {

    public static final UUID HR_USER = UUID.fromString("10000000-0000-4000-8000-000000000002");
    public static final UUID INTERVIEWER_USER = UUID.fromString("10000000-0000-4000-8000-000000000003");

    public static final UUID ALICE_CANDIDATE = UUID.fromString("20000000-0000-4000-8000-000000000001");

    public static final UUID JAVA_JOB = UUID.fromString("30000000-0000-4000-8000-000000000001");
    public static final UUID ALICE_APPLICATION = UUID.fromString("30000000-0000-4000-8000-000000000002");
    public static final UUID ALICE_ASSIGNMENT = UUID.fromString("30000000-0000-4000-8000-000000000003");
    public static final UUID ALICE_STAGE_APPLIED = UUID.fromString("30000000-0000-4000-8000-000000000004");
    public static final UUID ALICE_STAGE_INTERVIEW = UUID.fromString("30000000-0000-4000-8000-000000000005");

    private DemoIds() {
    }
}
