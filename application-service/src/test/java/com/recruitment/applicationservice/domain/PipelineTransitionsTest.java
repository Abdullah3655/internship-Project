package com.recruitment.applicationservice.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineTransitionsTest {

    @Test
    void allowsForwardSkip() {
        assertThat(PipelineTransitions.isAllowed(PipelineStage.APPLIED, PipelineStage.INTERVIEW)).isTrue();
    }

    @Test
    void rejectsReverseMove() {
        assertThat(PipelineTransitions.isAllowed(PipelineStage.INTERVIEW, PipelineStage.APPLIED)).isFalse();
    }

    @Test
    void hiredOnlyFromOffer() {
        assertThat(PipelineTransitions.isAllowed(PipelineStage.OFFER, PipelineStage.HIRED)).isTrue();
        assertThat(PipelineTransitions.isAllowed(PipelineStage.INTERVIEW, PipelineStage.HIRED)).isFalse();
    }

    @Test
    void disqualifiedCanReopenToApplied() {
        assertThat(PipelineTransitions.isAllowed(PipelineStage.DISQUALIFIED, PipelineStage.APPLIED)).isTrue();
    }

    @Test
    void hiredIsTerminal() {
        assertThat(PipelineTransitions.allowedFrom(PipelineStage.HIRED)).isEmpty();
        assertThat(PipelineTransitions.isTerminal(PipelineStage.HIRED)).isTrue();
        assertThat(PipelineTransitions.isTerminal(PipelineStage.DISQUALIFIED)).isTrue();
    }

    @Test
    void interviewWorkflowStages() {
        assertThat(PipelineTransitions.allowsInterviewerAssignment(PipelineStage.SCREENING)).isTrue();
        assertThat(PipelineTransitions.allowsInterviewerAssignment(PipelineStage.INTERVIEW)).isTrue();
        assertThat(PipelineTransitions.allowsInterviewerAssignment(PipelineStage.APPLIED)).isFalse();
        assertThat(PipelineTransitions.allowsInterviewerAssignment(PipelineStage.OFFER)).isFalse();
        assertThat(PipelineTransitions.allowsInterviewerAssignment(PipelineStage.HIRED)).isFalse();

        assertThat(PipelineTransitions.allowsEvaluation(PipelineStage.SCREENING)).isTrue();
        assertThat(PipelineTransitions.allowsEvaluation(PipelineStage.INTERVIEW)).isTrue();
        assertThat(PipelineTransitions.allowsEvaluation(PipelineStage.OFFER)).isFalse();
    }
}
