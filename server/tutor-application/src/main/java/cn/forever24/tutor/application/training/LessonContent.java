package cn.forever24.tutor.application.training;

import java.util.HashSet;
import java.util.List;

public record LessonContent(List<ComprehensionQuestion> questions, List<GuidedSpeakingTask> guidedSpeakingTasks) {
    public LessonContent {
        questions = List.copyOf(questions);
        guidedSpeakingTasks = List.copyOf(guidedSpeakingTasks);
        var ids = new HashSet<String>();
        questions.forEach(question -> {
            if (!ids.add(question.questionId())) throw new IllegalArgumentException("duplicate lesson task id");
        });
        guidedSpeakingTasks.forEach(task -> {
            if (!ids.add(task.taskId())) throw new IllegalArgumentException("duplicate lesson task id");
        });
    }
}
