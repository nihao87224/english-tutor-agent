package cn.forever24.tutor.application.training;

import cn.forever24.tutor.application.roleplay.RolePlayTask;

import java.util.HashSet;
import java.util.List;

public record LessonContent(
        List<ComprehensionQuestion> questions,
        List<GuidedSpeakingTask> guidedSpeakingTasks,
        RolePlayTask rolePlayTask
) {
    public LessonContent(List<ComprehensionQuestion> questions, List<GuidedSpeakingTask> guidedSpeakingTasks) {
        this(questions, guidedSpeakingTasks, null);
    }

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
        if (rolePlayTask != null && !ids.add(rolePlayTask.taskId())) {
            throw new IllegalArgumentException("duplicate lesson task id");
        }
    }
}
