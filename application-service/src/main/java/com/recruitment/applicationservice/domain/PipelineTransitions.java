package com.recruitment.applicationservice.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class PipelineTransitions {

    private static final List<PipelineStage> FORWARD = List.of(
            PipelineStage.APPLIED,
            PipelineStage.SCREENING,
            PipelineStage.INTERVIEW,
            PipelineStage.OFFER
    );

    private PipelineTransitions() {
    }

    public static boolean isAllowed(PipelineStage from, PipelineStage to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return allowedFrom(from).contains(to);
    }

    public static Set<PipelineStage> allowedFrom(PipelineStage from) {
        if (from == PipelineStage.HIRED) {
            return Set.of();
        }
        if (from == PipelineStage.DISQUALIFIED) {
            return Set.of(PipelineStage.APPLIED);
        }

        EnumSet<PipelineStage> next = EnumSet.of(PipelineStage.DISQUALIFIED);
        int fromIndex = FORWARD.indexOf(from);
        if (fromIndex < 0) {
            return Set.copyOf(next);
        }
        for (int i = fromIndex + 1; i < FORWARD.size(); i++) {
            next.add(FORWARD.get(i));
        }
        if (from == PipelineStage.OFFER) {
            next.add(PipelineStage.HIRED);
        }
        return Set.copyOf(next);
    }

    public static boolean isTerminal(PipelineStage stage) {
        return stage == PipelineStage.HIRED || stage == PipelineStage.DISQUALIFIED;
    }

    public static boolean allowsInterviewerAssignment(PipelineStage stage) {
        return stage == PipelineStage.SCREENING || stage == PipelineStage.INTERVIEW;
    }

    public static boolean allowsEvaluation(PipelineStage stage) {
        return stage == PipelineStage.SCREENING || stage == PipelineStage.INTERVIEW;
    }
}
