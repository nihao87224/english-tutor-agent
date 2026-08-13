package cn.forever24.tutor.application.conversation;

public interface CorrectionAnalyzer {

    LayeredCorrectionResult analyze(CorrectionAnalysisContext context);
}
