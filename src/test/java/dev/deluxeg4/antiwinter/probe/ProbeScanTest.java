package dev.deluxeg4.antiwinter.probe;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProbeScanTest {
    @Test void verificationRetryIsBoundedAndDoesNotCreateHits() {
        var scan = new ProbeScan(UUID.randomUUID(), null, null, true);
        assertTrue(scan.retryVerification());
        assertFalse(scan.retryVerification());
        assertFalse(scan.retryVerification());
        assertTrue(scan.confirmedHits().isEmpty());
    }

    @Test void detectionStillRequiresTwoPasses() {
        var scan = new ProbeScan(UUID.randomUUID(), null, null, true);
        scan.recordHit("meteor");
        assertTrue(scan.advancePass());
        assertTrue(scan.confirmedHits().isEmpty());
        scan.recordHit("meteor");
        assertFalse(scan.advancePass());
        assertTrue(scan.confirmedHits().contains("meteor"));
    }
    @Test void filteringMustRepeatForSameKeyAndEvidenceType() {
        var scan = new ProbeScan(UUID.randomUUID(), null, null, true);
        scan.recordHit("filter:meteor");
        assertTrue(scan.advancePass());
        assertEquals("meteor", scan.nextBatch().getFirst().id());
        scan.recordHit("meteor");
        assertFalse(scan.advancePass());
        assertTrue(scan.confirmedHits().isEmpty());

        var repeated = new ProbeScan(UUID.randomUUID(), null, null, true);
        repeated.recordHit("filter:meteor");
        assertTrue(repeated.advancePass());
        repeated.recordHit("filter:meteor");
        assertFalse(repeated.advancePass());
        assertTrue(repeated.confirmedHits().contains("filter:meteor"));
    }
}
