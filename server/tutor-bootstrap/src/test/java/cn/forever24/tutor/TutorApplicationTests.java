package cn.forever24.tutor;

import cn.forever24.tutor.ai.provider.ChatProvider;
import cn.forever24.tutor.ai.provider.SpeechToTextProvider;
import cn.forever24.tutor.ai.provider.TextToSpeechProvider;
import cn.forever24.tutor.application.assessment.OpenAnswerEvaluator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class TutorApplicationTests {

    @Autowired
    private ChatProvider chatProvider;

    @Autowired
    private SpeechToTextProvider speechToTextProvider;

    @Autowired
    private TextToSpeechProvider textToSpeechProvider;

    @Autowired
    private OpenAnswerEvaluator openAnswerEvaluator;

    @Test
    void contextLoads() {
        assertNotNull(chatProvider);
        assertNotNull(speechToTextProvider);
        assertNotNull(textToSpeechProvider);
        assertNotNull(openAnswerEvaluator);
    }
}
