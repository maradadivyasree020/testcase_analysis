package com.example.tools.llm.dto;

import java.util.List;

public class LlmResponse {

    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public String getFirstContentOrThrow() {
        if (choices == null || choices.isEmpty()
                || choices.get(0).getMessage() == null) {
            throw new IllegalStateException("LLM returned no choices/content");
        }
        return choices.get(0).getMessage().getContent();
    }

    public static class Choice {
        private LlmRequest.Message message;

        public LlmRequest.Message getMessage() {
            return message;
        }

        public void setMessage(LlmRequest.Message message) {
            this.message = message;
        }
    }
}
