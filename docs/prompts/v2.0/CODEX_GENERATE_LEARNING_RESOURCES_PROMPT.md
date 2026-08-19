# Codex Task Prompt - English Tutor Agent V2.0 Learning Resource Generation

## Role

You are the content generation engineer for English Tutor Agent V2.0.

Your task is to generate structured Scenario Lesson learning resources, not ordinary English lessons.

The goal of every lesson is:

```
Real scenario understanding
        ↓
Useful expressions
        ↓
Active speaking
        ↓
AI Role Play
        ↓
AI Feedback
        ↓
Learning Memory Update
```

---

# 1. Read Specification First

Before generating anything, read:

```
docs/design/v2.0/11_CODEX_SINGLE_FILE_RESOURCE_SPEC.md
```

This file is the only source of truth for:

- lesson list;
- scenario definitions;
- image requirements;
- audio requirements;
- JSON structure;
- validation rules.

Do not invent additional lessons.
Do not change lesson IDs.

---

# 2. Generation Scope

Generate MVP resources:

```
Level: B1
Lesson Count: 12
```

Output location:

```
resources/learning-content/v2.0/
```

---

# 3. Generation Workflow

For every lesson:

## Step 1
Understand:

- communication goal;
- skill tags;
- user role;
- AI role;
- expected speaking ability.

## Step 2
Generate metadata:

```
lesson.json
```

## Step 3
Generate conversation:

```
transcript.json
audio_script.txt
```

Requirements:

- natural spoken English;
- B1 difficulty;
- 60-90 seconds;
- realistic conversation;
- not textbook style.

## Step 4
Generate expressions:

```
expressions.json
```

Rules:

- 4-6 reusable expressions;
- include Chinese meaning;
- include usage notes.

## Step 5
Generate questions:

```
questions.json
```

Rules:

- 3 comprehension questions;
- check understanding of scenario and intent;
- do not create grammar quizzes.

## Step 6
Generate speaking tasks:

```
speaking_tasks.json
```

Every lesson must have:

1. Guided Speaking
2. Role Play

Each task must define:

- user role;
- AI role;
- objective;
- success criteria.

---

# 4. Image Generation

For every lesson:

Generate:

```
image_prompt.txt
image.webp
```

Image purpose:

The user should understand the scene within 1-2 seconds.

Style:

```
realistic photography
natural lighting
clean composition
professional educational material
```

Must:

- match communication scenario;
- show realistic people;
- show relationship between characters.

Must not:

- contain readable text;
- contain fake UI text;
- rely on image text to explain meaning.

---

# 5. Audio Generation

Generate:

```
audio_script.txt
audio.mp3
```

Rules:

- American neutral English;
- 2 speakers preferred;
- 60-90 seconds;
- natural conversation;
- transcript must exactly match audio script.

---

# 6. Output Structure

Each lesson:

```
lessons/{topic}/{lesson_slug}/b1/
├── lesson.json
├── transcript.json
├── expressions.json
├── questions.json
├── speaking_tasks.json
├── image_prompt.txt
├── audio_script.txt
└── review_notes.md
```

Media:

```
assets/images/{topic}/{lesson_slug}/b1/image.webp
assets/audio/{topic}/{lesson_slug}/b1/audio.mp3
```

---

# 7. Final Output

After generating all lessons, create:

```
catalog/index.json
reports/generation_report.md
```

Generation report must include:

- generated lesson count;
- missing files;
- asset paths;
- validation result;
- manual review suggestions.

---

# 8. Validation Checklist

Before completion verify:

- all 12 lessons generated;
- all JSON files valid;
- all referenced paths exist;
- images match scenarios;
- audio matches transcript;
- every lesson has speaking practice;
- every lesson has role play;
- every lesson has clear communication goal.

---

# 9. Important Constraints

Do NOT:

- generate grammar-only lessons;
- generate random beautiful images;
- create video resources;
- expand lesson scope without approval.

Priority:

```
Learning effectiveness
>
Data consistency
>
Asset quality
>
Quantity
```

Final objective:

> First make the learning loop work, then scale the number of lessons.
