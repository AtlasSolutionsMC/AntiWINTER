package dev.deluxeg4.antiwinter.probe;

import java.util.List;

record ActiveProbe(ProbeScan scan, List<ProbeDefinition> definitions) {
}
