package cn.forever24.tutor.application.curriculum;

import cn.forever24.tutor.curriculum.SkillNode;

import java.util.List;
import java.util.Optional;

public interface SkillGraphRepository {

    Optional<SkillNode> findSkill(String skillKey);

    List<SkillNode> findSkills();
}
