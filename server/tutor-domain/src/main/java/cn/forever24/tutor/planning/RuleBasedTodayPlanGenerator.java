package cn.forever24.tutor.planning;

import cn.forever24.tutor.profile.PrimaryGoal;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public final class RuleBasedTodayPlanGenerator {

    private RuleBasedTodayPlanGenerator() {
    }

    public static LearningPlan generate(LearningPlanContext context) {
        List<LearnerSkillState> priorities = context.skillStates().stream()
                .sorted(Comparator
                        .comparing(RuleBasedTodayPlanGenerator::priorityScore)
                        .thenComparing(Comparator.comparing(LearnerSkillState::confidence).reversed()))
                .limit(taskCount(context.dailyMinutes()))
                .toList();
        if (priorities.isEmpty()) {
            throw new IllegalArgumentException("skill states are required");
        }

        List<Integer> durations = durations(context.dailyMinutes(), priorities.size());
        List<LearningPlanTask> tasks = new java.util.ArrayList<>();
        for (int index = 0; index < priorities.size(); index++) {
            LearnerSkillState skill = priorities.get(index);
            tasks.add(taskFor(context.primaryGoal(), skill, durations.get(index), context.planId(), index + 1));
        }
        return new LearningPlan(
                context.planId(),
                context.date(),
                tasks.stream().mapToInt(LearningPlanTask::durationMinutes).sum(),
                tasks,
                reasons(context.primaryGoal(), priorities),
                false,
                context.profileVersion());
    }

    private static int taskCount(int dailyMinutes) {
        if (dailyMinutes <= 5) {
            return 1;
        }
        if (dailyMinutes <= 10) {
            return 2;
        }
        return 3;
    }

    private static List<Integer> durations(int totalMinutes, int taskCount) {
        if (taskCount == 1) {
            return List.of(totalMinutes);
        }
        if (taskCount == 2) {
            return List.of((int) Math.ceil(totalMinutes * 0.6), totalMinutes - (int) Math.ceil(totalMinutes * 0.6));
        }
        int first = Math.max(6, totalMinutes / 2);
        int second = Math.max(4, (totalMinutes - first) / 2);
        return List.of(first, second, totalMinutes - first - second);
    }

    private static BigDecimal priorityScore(LearnerSkillState skill) {
        int completedEvidenceAfterBaseline = Math.max(0, skill.evidenceCount() - 1);
        return skill.estimate().add(new BigDecimal("0.1000").multiply(BigDecimal.valueOf(completedEvidenceAfterBaseline)));
    }

    private static LearningPlanTask taskFor(
            PrimaryGoal goal,
            LearnerSkillState skill,
            int durationMinutes,
            String planId,
            int sequence
    ) {
        String taskType = taskType(goal, skill.dimension());
        return new LearningPlanTask(
                planId + "-task-" + sequence,
                taskType,
                title(goal, skill.dimension(), taskType),
                durationMinutes,
                List.of(skill.dimension()),
                difficulty(skill.estimate()),
                "根据初始画像，" + displayName(skill.dimension()) + " 当前估计为 " + skill.level() + "。");
    }

    private static String taskType(PrimaryGoal goal, String dimension) {
        if (goal == PrimaryGoal.IELTS && List.of("speaking", "fluency", "naturalness").contains(dimension)) {
            return "IELTS";
        }
        if (goal == PrimaryGoal.WORKPLACE && List.of("speaking", "listening", "naturalness").contains(dimension)) {
            return "CONVERSATION";
        }
        return switch (dimension) {
            case "listening" -> "LISTENING";
            case "speaking", "fluency", "naturalness" -> "SPEAKING";
            case "reading", "vocabulary" -> "READING";
            case "writing", "grammar" -> "WRITING";
            default -> "SUMMARY";
        };
    }

    private static String title(PrimaryGoal goal, String dimension, String taskType) {
        if (goal == PrimaryGoal.WORKPLACE && "CONVERSATION".equals(taskType)) {
            return "工作场景快速回应";
        }
        if (goal == PrimaryGoal.IELTS && "IELTS".equals(taskType)) {
            return "IELTS 口语短回答";
        }
        return switch (dimension) {
            case "listening" -> "听懂关键信息";
            case "speaking" -> "短句回答练习";
            case "reading" -> "阅读定位练习";
            case "writing" -> "短写作打磨";
            case "grammar" -> "语法准确性训练";
            case "vocabulary" -> "高频词块复用";
            case "fluency" -> "流畅表达练习";
            case "naturalness" -> "自然表达替换";
            default -> "今日学习总结";
        };
    }

    private static List<String> reasons(PrimaryGoal goal, List<LearnerSkillState> priorities) {
        String firstSkill = displayName(priorities.get(0).dimension());
        String goalText = switch (goal) {
            case WORKPLACE -> "你的主要目标是工作沟通";
            case IELTS -> "你的主要目标是 IELTS 练习";
            case GENERAL -> "你的主要目标是综合提升";
        };
        return List.of(
                goalText + "，今日优先处理 " + firstSkill + "。",
                "计划依据初始画像中的低分维度生成，后续会随训练证据调整。");
    }

    private static String difficulty(BigDecimal estimate) {
        if (estimate.compareTo(new BigDecimal("0.7000")) >= 0) {
            return "HARD";
        }
        if (estimate.compareTo(new BigDecimal("0.4500")) >= 0) {
            return "MEDIUM";
        }
        return "EASY";
    }

    private static String displayName(String dimension) {
        return switch (dimension) {
            case "listening" -> "听力";
            case "speaking" -> "口语";
            case "reading" -> "阅读";
            case "writing" -> "写作";
            case "grammar" -> "语法";
            case "vocabulary" -> "词汇";
            case "fluency" -> "流畅度";
            case "naturalness" -> "自然度";
            default -> dimension;
        };
    }
}
