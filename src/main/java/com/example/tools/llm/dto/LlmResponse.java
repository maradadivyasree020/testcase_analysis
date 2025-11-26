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
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("LLM returned no choices");
        }
        Choice choice = choices.get(0);
        if (choice.getMessage() == null || choice.getMessage().getContent() == null) {
            throw new IllegalStateException("LLM returned empty content");
        }
        return choice.getMessage().getContent();
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
