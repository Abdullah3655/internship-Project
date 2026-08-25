package com.recruitment.candidateservice.config;

import com.recruitment.candidateservice.domain.Candidate;
import com.recruitment.candidateservice.domain.CandidateSource;
import com.recruitment.candidateservice.domain.Tag;
import com.recruitment.candidateservice.domain.TalentStatus;
import com.recruitment.candidateservice.repository.CandidateRepository;
import com.recruitment.candidateservice.repository.TagRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("!test")
public class DemoCandidateSeeder implements ApplicationRunner {

    private final CandidateRepository candidateRepository;
    private final TagRepository tagRepository;

    public DemoCandidateSeeder(CandidateRepository candidateRepository, TagRepository tagRepository) {
        this.candidateRepository = candidateRepository;
        this.tagRepository = tagRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCandidate(
                DemoIds.ALICE,
                "Alice",
                "Smith",
                "alice@example.com",
                "+1234567890",
                List.of("java", "spring")
        );
        seedCandidate(
                DemoIds.BOB,
                "Bob",
                "Jones",
                "bob@example.com",
                "+15551234567",
                List.of("python", "sql")
        );
    }

    private void seedCandidate(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            List<String> tagNames
    ) {
        if (candidateRepository.existsById(id)
                || candidateRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(email)) {
            return;
        }
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setFirstName(firstName);
        candidate.setLastName(lastName);
        candidate.setEmail(email.toLowerCase(Locale.ROOT));
        candidate.setPhone(phone);
        candidate.setSource(CandidateSource.MANUAL);
        candidate.setTalentStatus(TalentStatus.IN_POOL);
        candidate.setCreatedByUserId(DemoIds.HR_USER);
        candidate.setTags(resolveTags(tagNames));
        candidateRepository.save(candidate);
    }

    private Set<Tag> resolveTags(List<String> rawTags) {
        Set<Tag> tags = new LinkedHashSet<>();
        for (String raw : rawTags) {
            String name = raw.trim().toLowerCase(Locale.ROOT);
            Tag tag = tagRepository.findByNameIgnoreCase(name).orElseGet(() -> {
                Tag created = new Tag();
                created.setName(name);
                return tagRepository.save(created);
            });
            tags.add(tag);
        }
        return tags;
    }
}
