package cn.forever24.tutor.application.training;

import cn.forever24.tutor.training.AttemptAnalysis;

@FunctionalInterface
public interface SpeakingAttemptAnalyzer {
    AttemptAnalysis analyze(SpeakingAttemptAnalysisContext context);
}
